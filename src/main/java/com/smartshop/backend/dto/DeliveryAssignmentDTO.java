package com.smartshop.backend.dto;

import com.smartshop.backend.entity.DeliveryStatus;
import com.smartshop.backend.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignmentDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String deliveryInstructions;
    private Double totalAmount;
    private String paymentMethod;
    private String paymentStatus;

    @Builder.Default
    private List<OrderItemDTO> items = new ArrayList<>();

    private Long agentId;
    private String agentName;
    private String agentPhone;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String notes;
    private DeliveryLocationDTO latestLocation;
}
