package com.smartshop.backend.service;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.entity.*;
import com.smartshop.backend.exception.BadRequestException;
import com.smartshop.backend.exception.ResourceNotFoundException;
import com.smartshop.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryLocationRepository locationRepository;
    private final CartService cartService;

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

        Double destLat = request.getDestinationLatitude() != null ? request.getDestinationLatitude() : 17.3850;
        Double destLng = request.getDestinationLongitude() != null ? request.getDestinationLongitude() : 78.4867;
        LocalDateTime estDelivery = LocalDateTime.now().plusDays(2).withHour(18).withMinute(0);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .totalAmount(cart.getTotal())
                .deliveryFee(cart.getDeliveryFee())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(paymentStatus)
                .orderStatus(OrderStatus.PROCESSING)
                .shippingAddress(request.getShippingAddress())
                .destinationLatitude(destLat)
                .destinationLongitude(destLng)
                .estimatedDeliveryTime(estDelivery)
                .deliveryInstructions(request.getDeliveryInstructions())
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

        // Cancel delivery assignment if exists
        assignmentRepository.findByOrderId(order.getId()).ifPresent(assignment -> {
            assignment.setDeliveryStatus(DeliveryStatus.CANCELLED);
            assignmentRepository.save(assignment);
        });

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

    // ─── REORDER & RECENT PRODUCTS IMPLEMENTATION ────────────────────────────

    @Override
    public List<OrderDTO> getRecentOrdersForUser(String userEmail, int limit) {
        User user = getUserByEmail(userEmail);
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (limit > 0 && orders.size() > limit) {
            orders = orders.subList(0, limit);
        }
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<RecentPurchasedProductDTO> getRecentPurchasedProducts(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Track products seen so far (deduplication by productId)
        Map<Long, RecentPurchasedProductDTO> productMap = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getItems() == null) continue;
            for (OrderItem item : order.getItems()) {
                Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
                if (productId == null || productMap.containsKey(productId)) {
                    continue;
                }

                // Fetch latest live product details
                Optional<Product> liveProductOpt = productRepository.findById(productId);
                if (liveProductOpt.isEmpty()) {
                    continue;
                }
                Product liveProduct = liveProductOpt.get();

                double currentPrice = liveProduct.getPrice();
                double prevPrice = item.getPrice();
                boolean priceChanged = Math.abs(currentPrice - prevPrice) > 0.01;
                boolean inStock = liveProduct.getStock() > 0;

                RecentPurchasedProductDTO dto = RecentPurchasedProductDTO.builder()
                        .productId(liveProduct.getId())
                        .productName(liveProduct.getName())
                        .productImage(liveProduct.getImage())
                        .brand(liveProduct.getBrand() != null ? liveProduct.getBrand() : "SmartShop")
                        .categoryName(liveProduct.getCategory() != null ? liveProduct.getCategory().getName() : "")
                        .currentPrice(currentPrice)
                        .previousPrice(prevPrice)
                        .priceChanged(priceChanged)
                        .previousPurchaseDate(order.getCreatedAt())
                        .previousQuantity(item.getQuantity())
                        .stock(liveProduct.getStock())
                        .inStock(inStock)
                        .rating(liveProduct.getRating())
                        .reviewCount(liveProduct.getReviewCount())
                        .previousOrderNumber(order.getOrderNumber())
                        .previousOrderId(order.getId())
                        .build();

                productMap.put(productId, dto);
            }
        }

        return new ArrayList<>(productMap.values());
    }

    @Override
    public ReorderSummaryResponse getReorderSummary(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        validateOrderOwnership(order, userEmail);

        List<ReorderItemDTO> reorderItems = new ArrayList<>();
        boolean allAvailable = true;
        boolean hasPriceChanges = false;
        int unavailableCount = 0;
        double eligibleSubtotal = 0.0;

        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
            Optional<Product> liveProductOpt = (productId != null) ? productRepository.findById(productId) : Optional.empty();

            if (liveProductOpt.isEmpty()) {
                // Product discontinued
                allAvailable = false;
                unavailableCount++;
                reorderItems.add(ReorderItemDTO.builder()
                        .productId(productId)
                        .productName(item.getProductName())
                        .productImage(item.getProductImage())
                        .brand("")
                        .previousPrice(item.getPrice())
                        .currentPrice(item.getPrice())
                        .priceChanged(false)
                        .priceDifference(0.0)
                        .quantity(item.getQuantity())
                        .availableStock(0)
                        .inStock(false)
                        .discontinued(true)
                        .rating(0.0)
                        .build());
                continue;
            }

            Product liveProduct = liveProductOpt.get();
            double curPrice = liveProduct.getPrice();
            double prevPrice = item.getPrice();
            boolean changed = Math.abs(curPrice - prevPrice) > 0.01;
            if (changed) hasPriceChanges = true;
            double diff = curPrice - prevPrice;

            int availableStock = liveProduct.getStock();
            boolean inStock = availableStock > 0;
            if (!inStock) {
                allAvailable = false;
                unavailableCount++;
            } else {
                eligibleSubtotal += curPrice * Math.min(item.getQuantity(), availableStock);
            }

            reorderItems.add(ReorderItemDTO.builder()
                    .productId(liveProduct.getId())
                    .productName(liveProduct.getName())
                    .productImage(liveProduct.getImage())
                    .brand(liveProduct.getBrand() != null ? liveProduct.getBrand() : "")
                    .previousPrice(prevPrice)
                    .currentPrice(curPrice)
                    .priceChanged(changed)
                    .priceDifference(diff)
                    .quantity(item.getQuantity())
                    .availableStock(availableStock)
                    .inStock(inStock)
                    .discontinued(false)
                    .rating(liveProduct.getRating())
                    .build());
        }

        String priceChangeMessage = hasPriceChanges
                ? "Note: Current prices for some products have changed since your previous purchase."
                : null;

        return ReorderSummaryResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .items(reorderItems)
                .allItemsAvailable(allAvailable)
                .hasPriceChanges(hasPriceChanges)
                .priceChangeMessage(priceChangeMessage)
                .unavailableItemsCount(unavailableCount)
                .eligibleSubtotal(eligibleSubtotal)
                .build();
    }

    @Override
    @Transactional
    public ReorderSummaryResponse executeReorder(Long orderId, String userEmail, ReorderExecuteRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        validateOrderOwnership(order, userEmail);

        User user = order.getUser();

        // Retrieve existing cart or create empty one
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).subtotal(0.0).deliveryFee(0.0).total(0.0).build();
                    return cartRepository.save(newCart);
                });

        Map<Long, Integer> customQuantities = (request != null && request.getItemQuantities() != null)
                ? request.getItemQuantities()
                : Collections.emptyMap();

        List<ReorderItemDTO> itemSummaries = new ArrayList<>();
        boolean allAvailable = true;
        boolean hasPriceChanges = false;
        int unavailableCount = 0;

        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
            if (productId == null) continue;

            Optional<Product> liveProductOpt = productRepository.findById(productId);
            if (liveProductOpt.isEmpty()) {
                allAvailable = false;
                unavailableCount++;
                continue;
            }

            Product product = liveProductOpt.get();
            int requestedQty = customQuantities.getOrDefault(productId, item.getQuantity());
            if (requestedQty <= 0) continue;

            int stock = product.getStock();
            if (stock <= 0) {
                allAvailable = false;
                unavailableCount++;
                continue;
            }

            int finalQty = Math.min(requestedQty, stock);

            // Add or update in user's cart (preserving existing cart items!)
            Optional<CartItem> existingCartItem = cart.getItems().stream()
                    .filter(ci -> ci.getProduct().getId().equals(product.getId()))
                    .findFirst();

            if (existingCartItem.isPresent()) {
                CartItem ci = existingCartItem.get();
                int combinedQty = Math.min(ci.getQuantity() + finalQty, stock);
                ci.setQuantity(combinedQty);
                ci.setPrice(product.getPrice());
                cartItemRepository.save(ci);
            } else {
                CartItem newCi = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(finalQty)
                        .price(product.getPrice())
                        .build();
                CartItem savedCi = cartItemRepository.save(newCi);
                cart.getItems().add(savedCi);
            }

            double curPrice = product.getPrice();
            double prevPrice = item.getPrice();
            boolean changed = Math.abs(curPrice - prevPrice) > 0.01;
            if (changed) hasPriceChanges = true;

            itemSummaries.add(ReorderItemDTO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(product.getImage())
                    .brand(product.getBrand())
                    .previousPrice(prevPrice)
                    .currentPrice(curPrice)
                    .priceChanged(changed)
                    .priceDifference(curPrice - prevPrice)
                    .quantity(finalQty)
                    .availableStock(stock)
                    .inStock(true)
                    .discontinued(false)
                    .rating(product.getRating())
                    .build());
        }

        cart.recalculateTotals();
        cartRepository.save(cart);

        CartDTO updatedCartDTO = cartService.getCartByUserId(user.getId());

        String message = hasPriceChanges
                ? "Items added to cart with current prices updated."
                : "Eligible items successfully added to your shopping cart.";

        return ReorderSummaryResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .items(itemSummaries)
                .allItemsAvailable(allAvailable)
                .hasPriceChanges(hasPriceChanges)
                .priceChangeMessage(message)
                .unavailableItemsCount(unavailableCount)
                .eligibleSubtotal(updatedCartDTO.getSubtotal())
                .updatedCart(updatedCartDTO)
                .build();
    }

    // ─── ORDER TRACKING IMPLEMENTATION ────────────────────────────────────────

    @Override
    public OrderTrackingDTO getOrderTracking(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        validateOrderTrackingAccess(order, userEmail);

        // Fetch assignment if any
        Optional<DeliveryAssignment> assignmentOpt = assignmentRepository.findByOrderId(order.getId());
        DeliveryStatus deliveryStatus = assignmentOpt.map(DeliveryAssignment::getDeliveryStatus)
                .orElse(DeliveryStatus.PENDING_ASSIGNMENT);

        DeliveryAgentDTO agentDTO = null;
        DeliveryLocationDTO latestLocation = null;
        List<DeliveryLocationDTO> recentLocations = new ArrayList<>();
        boolean isLiveActive = false;

        if (assignmentOpt.isPresent()) {
            DeliveryAssignment assignment = assignmentOpt.get();
            User agent = assignment.getDeliveryAgent();
            agentDTO = DeliveryAgentDTO.builder()
                    .id(agent.getId())
                    .fullName(agent.getFullName())
                    .phoneNumber(agent.getPhoneNumber())
                    .email(agent.getEmail())
                    .build();

            // Fetch latest location
            Optional<DeliveryLocation> locOpt = locationRepository.findFirstByDeliveryAssignmentIdOrderByRecordedAtDesc(assignment.getId());
            if (locOpt.isPresent()) {
                DeliveryLocation loc = locOpt.get();
                long secondsAgo = Duration.between(loc.getRecordedAt(), LocalDateTime.now()).getSeconds();
                latestLocation = DeliveryLocationDTO.builder()
                        .latitude(loc.getLatitude())
                        .longitude(loc.getLongitude())
                        .accuracy(loc.getAccuracy())
                        .recordedAt(loc.getRecordedAt())
                        .secondsAgo(secondsAgo)
                        .isStale(secondsAgo > 300)
                        .build();
            }

            // Fetch recent 10 locations for route trail
            List<DeliveryLocation> locHistory = locationRepository.findTop20ByDeliveryAssignmentIdOrderByRecordedAtDesc(assignment.getId());
            for (DeliveryLocation loc : locHistory) {
                long secondsAgo = Duration.between(loc.getRecordedAt(), LocalDateTime.now()).getSeconds();
                recentLocations.add(DeliveryLocationDTO.builder()
                        .latitude(loc.getLatitude())
                        .longitude(loc.getLongitude())
                        .accuracy(loc.getAccuracy())
                        .recordedAt(loc.getRecordedAt())
                        .secondsAgo(secondsAgo)
                        .isStale(secondsAgo > 300)
                        .build());
            }

            isLiveActive = (order.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY &&
                    assignment.getDeliveryStatus() == DeliveryStatus.OUT_FOR_DELIVERY);
        }

        // Default destination coordinates if missing
        Double destLat = order.getDestinationLatitude() != null ? order.getDestinationLatitude() : 17.3850;
        Double destLng = order.getDestinationLongitude() != null ? order.getDestinationLongitude() : 78.4867;

        return OrderTrackingDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .deliveryStatus(deliveryStatus)
                .createdAt(order.getCreatedAt())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .shippingAddress(order.getShippingAddress())
                .destinationLatitude(destLat)
                .destinationLongitude(destLng)
                .deliveryInstructions(order.getDeliveryInstructions())
                .items(order.getItems().stream().map(this::mapItemToDTO).collect(Collectors.toList()))
                .deliveryAgent(agentDTO)
                .latestLocation(latestLocation)
                .recentLocations(recentLocations)
                .isLiveTrackingActive(isLiveActive)
                .lastUpdatedTimestamp(latestLocation != null ? latestLocation.getRecordedAt() : order.getUpdatedAt())
                .cancellationReason(order.getCancellationReason())
                .cancellationNote(order.getCancellationNote())
                .cancelledAt(order.getCancelledAt())
                .build();
    }

    @Override
    public DeliveryLocationDTO getLatestTrackingLocation(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        validateOrderTrackingAccess(order, userEmail);

        Optional<DeliveryAssignment> assignmentOpt = assignmentRepository.findByOrderId(order.getId());
        if (assignmentOpt.isEmpty()) {
            return null;
        }

        DeliveryAssignment assignment = assignmentOpt.get();
        Optional<DeliveryLocation> locOpt = locationRepository.findFirstByDeliveryAssignmentIdOrderByRecordedAtDesc(assignment.getId());

        if (locOpt.isEmpty()) {
            return null;
        }

        DeliveryLocation loc = locOpt.get();
        long secondsAgo = Duration.between(loc.getRecordedAt(), LocalDateTime.now()).getSeconds();

        return DeliveryLocationDTO.builder()
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .accuracy(loc.getAccuracy())
                .recordedAt(loc.getRecordedAt())
                .secondsAgo(secondsAgo)
                .isStale(secondsAgo > 300)
                .build();
    }

    // ─── HELPER METHODS ──────────────────────────────────────────────────────

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void validateOrderOwnership(Order order, String userEmail) {
        if (userEmail == null || order.getUser() == null || !order.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            // Check if admin
            User caller = userRepository.findByEmail(userEmail).orElse(null);
            if (caller == null || caller.getRole() != Role.ROLE_ADMIN) {
                throw new BadRequestException("You are not authorized to access this order.");
            }
        }
    }

    private void validateOrderTrackingAccess(Order order, String userEmail) {
        if (userEmail == null) {
            throw new BadRequestException("Authentication required to view tracking.");
        }
        User caller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (caller.getRole() == Role.ROLE_ADMIN) return;

        // Check if assigned agent
        Optional<DeliveryAssignment> assignment = assignmentRepository.findByOrderId(order.getId());
        if (assignment.isPresent() && assignment.get().getDeliveryAgent().getId().equals(caller.getId())) {
            return;
        }

        // Check if customer owner
        if (order.getUser() != null && order.getUser().getId().equals(caller.getId())) {
            return;
        }

        throw new BadRequestException("You are not authorized to track this order.");
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
                .destinationLatitude(order.getDestinationLatitude())
                .destinationLongitude(order.getDestinationLongitude())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .deliveryInstructions(order.getDeliveryInstructions())
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
