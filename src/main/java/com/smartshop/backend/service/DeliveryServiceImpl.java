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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryLocationRepository locationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public List<DeliveryAssignmentDTO> getAssignedOrdersForAgent(String agentEmail) {
        User agent = getAgentByEmail(agentEmail);
        List<DeliveryAssignment> assignments = assignmentRepository.findByDeliveryAgentIdOrderByAssignedAtDesc(agent.getId());
        return assignments.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public DeliveryAssignmentDTO getAgentOrderDetails(Long orderId, String agentEmail) {
        User agent = getAgentByEmail(agentEmail);
        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAssignment", "orderId", orderId));

        if (!assignment.getDeliveryAgent().getId().equals(agent.getId())) {
            throw new BadRequestException("You are not authorized to view this delivery assignment.");
        }

        return mapToDTO(assignment);
    }

    @Override
    @Transactional
    public DeliveryAssignmentDTO startDelivery(Long orderId, String agentEmail) {
        User agent = getAgentByEmail(agentEmail);
        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAssignment", "orderId", orderId));

        if (!assignment.getDeliveryAgent().getId().equals(agent.getId())) {
            throw new BadRequestException("You are not authorized to start this delivery.");
        }

        Order order = assignment.getOrder();
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot start delivery for a cancelled order.");
        }
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order is already delivered.");
        }

        assignment.setDeliveryStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        assignment.setStartedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);

        return mapToDTO(assignment);
    }

    @Override
    @Transactional
    public DeliveryLocationDTO updateLocation(Long orderId, String agentEmail, DeliveryLocationRequest request) {
        User agent = getAgentByEmail(agentEmail);
        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAssignment", "orderId", orderId));

        if (!assignment.getDeliveryAgent().getId().equals(agent.getId())) {
            throw new BadRequestException("You are not the assigned delivery agent for this order.");
        }

        if (assignment.getDeliveryStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new BadRequestException("Location updates are only permitted while order is actively out for delivery.");
        }

        if (request.getLatitude() == null || request.getLatitude() < -90 || request.getLatitude() > 90) {
            throw new BadRequestException("Latitude must be between -90 and 90 degrees.");
        }
        if (request.getLongitude() == null || request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new BadRequestException("Longitude must be between -180 and 180 degrees.");
        }

        LocalDateTime recordedAt = request.getRecordedAt() != null ? request.getRecordedAt() : LocalDateTime.now();

        DeliveryLocation location = DeliveryLocation.builder()
                .deliveryAssignment(assignment)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracy(request.getAccuracy())
                .recordedAt(recordedAt)
                .build();

        DeliveryLocation saved = locationRepository.save(location);

        return DeliveryLocationDTO.builder()
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .accuracy(saved.getAccuracy())
                .recordedAt(saved.getRecordedAt())
                .isStale(false)
                .secondsAgo(0L)
                .build();
    }

    @Override
    @Transactional
    public DeliveryAssignmentDTO completeDelivery(Long orderId, String agentEmail) {
        User agent = getAgentByEmail(agentEmail);
        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAssignment", "orderId", orderId));

        if (!assignment.getDeliveryAgent().getId().equals(agent.getId())) {
            throw new BadRequestException("You are not authorized to complete this delivery.");
        }

        Order order = assignment.getOrder();
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot complete delivery for a cancelled order.");
        }

        assignment.setDeliveryStatus(DeliveryStatus.DELIVERED);
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        return mapToDTO(assignment);
    }

    @Override
    public List<OrderDTO> getUnassignedOrders() {
        List<Order> eligibleOrders = orderRepository.findByOrderStatusInOrderByCreatedAtDesc(
                List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED)
        );

        return eligibleOrders.stream()
                .filter(order -> assignmentRepository.findByOrderId(order.getId()).isEmpty())
                .map(this::mapOrderToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliveryAgentDTO> getAvailableDeliveryAgents() {
        List<User> agents = userRepository.findByRole(Role.ROLE_DELIVERY_AGENT);
        return agents.stream().map(a -> DeliveryAgentDTO.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phoneNumber(a.getPhoneNumber())
                .email(a.getEmail())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public List<DeliveryAssignmentDTO> getAllAssignments() {
        List<DeliveryAssignment> list = assignmentRepository.findAllByOrderByAssignedAtDesc();
        return list.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DeliveryAssignmentDTO assignDeliveryAgent(AssignDeliveryRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot assign delivery agent to a cancelled order.");
        }
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order is already delivered.");
        }

        User agent = userRepository.findById(request.getDeliveryAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getDeliveryAgentId()));

        if (agent.getRole() != Role.ROLE_DELIVERY_AGENT) {
            throw new BadRequestException("User is not registered as a delivery agent.");
        }

        Optional<DeliveryAssignment> existing = assignmentRepository.findByOrderId(order.getId());
        DeliveryAssignment assignment;
        if (existing.isPresent()) {
            assignment = existing.get();
            assignment.setDeliveryAgent(agent);
            assignment.setDeliveryStatus(DeliveryStatus.ASSIGNED);
            assignment.setAssignedAt(LocalDateTime.now());
            if (request.getNotes() != null) assignment.setNotes(request.getNotes());
        } else {
            assignment = DeliveryAssignment.builder()
                    .order(order)
                    .deliveryAgent(agent)
                    .deliveryStatus(DeliveryStatus.ASSIGNED)
                    .assignedAt(LocalDateTime.now())
                    .notes(request.getNotes())
                    .build();
        }

        if (order.getOrderStatus() == OrderStatus.PLACED || order.getOrderStatus() == OrderStatus.PROCESSING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }

        DeliveryAssignment saved = assignmentRepository.save(assignment);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public DeliveryAssignmentDTO reassignDeliveryAgent(AssignDeliveryRequest request) {
        return assignDeliveryAgent(request);
    }

    private User getAgentByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (user.getRole() != Role.ROLE_DELIVERY_AGENT && user.getRole() != Role.ROLE_ADMIN) {
            throw new BadRequestException("Access denied: Not a delivery agent account.");
        }
        return user;
    }

    private DeliveryAssignmentDTO mapToDTO(DeliveryAssignment assignment) {
        Order o = assignment.getOrder();
        User agent = assignment.getDeliveryAgent();

        DeliveryLocationDTO latestLoc = null;
        Optional<DeliveryLocation> locOpt = locationRepository.findFirstByDeliveryAssignmentIdOrderByRecordedAtDesc(assignment.getId());
        if (locOpt.isPresent()) {
            DeliveryLocation loc = locOpt.get();
            long secondsAgo = Duration.between(loc.getRecordedAt(), LocalDateTime.now()).getSeconds();
            boolean isStale = secondsAgo > 300; // > 5 minutes
            latestLoc = DeliveryLocationDTO.builder()
                    .latitude(loc.getLatitude())
                    .longitude(loc.getLongitude())
                    .accuracy(loc.getAccuracy())
                    .recordedAt(loc.getRecordedAt())
                    .secondsAgo(secondsAgo)
                    .isStale(isStale)
                    .build();
        }

        List<OrderItemDTO> items = (o != null && o.getItems() != null)
                ? o.getItems().stream().map(i -> OrderItemDTO.builder()
                        .id(i.getId())
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .productName(i.getProductName())
                        .productImage(i.getProductImage())
                        .price(i.getPrice())
                        .quantity(i.getQuantity())
                        .itemTotal(i.getPrice() * i.getQuantity())
                        .build()).collect(Collectors.toList())
                : List.of();

        return DeliveryAssignmentDTO.builder()
                .id(assignment.getId())
                .orderId(o != null ? o.getId() : null)
                .orderNumber(o != null ? o.getOrderNumber() : "")
                .orderStatus(o != null ? o.getOrderStatus() : null)
                .deliveryStatus(assignment.getDeliveryStatus())
                .customerName(o != null && o.getUser() != null ? o.getUser().getFullName() : "Customer")
                .customerPhone(o != null && o.getUser() != null ? o.getUser().getPhoneNumber() : "")
                .shippingAddress(o != null ? o.getShippingAddress() : "")
                .destinationLatitude(o != null ? o.getDestinationLatitude() : null)
                .destinationLongitude(o != null ? o.getDestinationLongitude() : null)
                .deliveryInstructions(o != null ? o.getDeliveryInstructions() : "")
                .totalAmount(o != null ? o.getTotalAmount() : 0.0)
                .paymentMethod(o != null && o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "")
                .paymentStatus(o != null && o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "")
                .items(items)
                .agentId(agent != null ? agent.getId() : null)
                .agentName(agent != null ? agent.getFullName() : "")
                .agentPhone(agent != null ? agent.getPhoneNumber() : "")
                .assignedAt(assignment.getAssignedAt())
                .startedAt(assignment.getStartedAt())
                .completedAt(assignment.getCompletedAt())
                .notes(assignment.getNotes())
                .latestLocation(latestLoc)
                .build();
    }

    private OrderDTO mapOrderToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .customerName(order.getUser() != null ? order.getUser().getFullName() : "")
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : "")
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
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
