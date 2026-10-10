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
public class OrderTrackingDTO {
    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDeliveryTime;
    private String shippingAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String deliveryInstructions;

    @Builder.Default
    private List<OrderItemDTO> items = new ArrayList<>();

    private DeliveryAgentDTO deliveryAgent;
    private DeliveryLocationDTO latestLocation;

    @Builder.Default
    private List<DeliveryLocationDTO> recentLocations = new ArrayList<>();

    private Boolean isLiveTrackingActive;
    private LocalDateTime lastUpdatedTimestamp;

    // Cancellation details
    private String cancellationReason;
    private String cancellationNote;
    private LocalDateTime cancelledAt;
}
