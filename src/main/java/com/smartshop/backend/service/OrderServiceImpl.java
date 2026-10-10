package com.smartshop.backend.service;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.entity.*;
import com.smartshop.backend.exception.BadRequestException;
import com.smartshop.backend.exception.ResourceNotFoundException;
import com.smartshop.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** Statuses that allow a customer-initiated cancellation. */
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PLACED,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING
    );

    @Override
    @Transactional
    public OrderDTO placeOrder(PlaceOrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Shopping cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place order with an empty cart");
        }

        // Validate stock for all items
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product '" + product.getName() + "'. Available: " + product.getStock());
            }
        }

        // Generate unique order number
        String orderNumber = generateOrderNumber();

        // Calculate totals
        cart.recalculateTotals();

        PaymentStatus paymentStatus = (request.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY)
                ? PaymentStatus.PENDING
                : PaymentStatus.PAID;

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .totalAmount(cart.getTotal())
                .deliveryFee(cart.getDeliveryFee())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(paymentStatus)
                .orderStatus(OrderStatus.PROCESSING)
                .shippingAddress(request.getShippingAddress())
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Deduct stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .productName(product.getName())
                    .productImage(product.getImage())
                    .build();

            orderItems.add(orderItemRepository.save(orderItem));
        }

        savedOrder.setItems(orderItems);

        // Clear user cart
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cart.setSubtotal(0.0);
        cart.setDeliveryFee(0.0);
        cart.setTotal(0.0);
        cartRepository.save(cart);

        return mapToDTO(savedOrder);
    }

    @Override
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToDTO(order);
    }

    @Override
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return mapToDTO(order);
    }

    @Override
    public List<OrderDTO> getOrdersByUserId(Long userId, String status) {
        List<Order> orders;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(userId, orderStatus);
            } catch (IllegalArgumentException e) {
                orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            }
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getAllOrders(String status) {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            orders = orders.stream()
                    .filter(o -> o.getOrderStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (request.getOrderStatus() != null) {
            order.setOrderStatus(request.getOrderStatus());
            if (request.getOrderStatus() == OrderStatus.DELIVERED) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        }

        if (request.getPaymentStatus() != null) {
            order.setPaymentStatus(request.getPaymentStatus());
        }

        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // 1. Ownership check — the requesting user must own this order
        if (order.getUser() == null || !order.getUser().getId().equals(request.getUserId())) {
            throw new BadRequestException("You are not authorized to cancel this order.");
        }

        // 2. Status check
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("This order has already been cancelled.");
        }
        if (!CANCELLABLE_STATUSES.contains(currentStatus)) {
            throw new BadRequestException(
                    "This order can no longer be cancelled because it has already been " +
                    currentStatus.name().toLowerCase().replace('_', ' ') + "."
            );
        }

        // 3. Restore inventory for each item
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        // 4. Mark order as cancelled
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getCancellationReason());
        order.setCancellationNote(request.getCancellationNote());
        order.setCancelledAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        return mapToDTO(saved);
    }

    @Override
    public DashboardStatsDTO getAdminDashboardStats() {
        long totalProducts = productRepository.count();
        long totalUsers = userRepository.count();
        List<Order> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();

        double totalRevenue = allOrders.stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED)
                .mapToDouble(Order::getTotalAmount)
                .sum();

        long processing = allOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.PROCESSING).count();
        long shipped = allOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.SHIPPED).count();
        long delivered = allOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED).count();
        long cancelled = allOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.CANCELLED).count();

        return DashboardStatsDTO.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalUsers(totalUsers)
                .totalRevenue(totalRevenue)
                .processingOrders(processing)
                .shippedOrders(shipped)
                .deliveredOrders(delivered)
                .cancelledOrders(cancelled)
                .build();
    }

    private String generateOrderNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = 1000 + new Random().nextInt(9000);
        return "ORD-" + dateStr + "-" + randomNum;
    }

    private OrderDTO mapToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .customerName(order.getUser() != null ? order.getUser().getFullName() : "Guest")
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : "")
                .items(order.getItems().stream().map(this::mapItemToDTO).collect(Collectors.toList()))
                .totalAmount(order.getTotalAmount())
                .deliveryFee(order.getDeliveryFee())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .shippingAddress(order.getShippingAddress())
                .cancellationReason(order.getCancellationReason())
                .cancellationNote(order.getCancellationNote())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemDTO mapItemToDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .itemTotal(item.getPrice() * item.getQuantity())
                .build();
    }
}
