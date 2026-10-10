package com.smartshop.backend.dto;

import com.smartshop.backend.entity.OrderStatus;
import com.smartshop.backend.entity.PaymentMethod;
import com.smartshop.backend.entity.PaymentStatus;
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
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String customerName;
    private String customerEmail;
    @Builder.Default
    private List<OrderItemDTO> items = new ArrayList<>();
    private Double totalAmount;
    private Double deliveryFee;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private String shippingAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private LocalDateTime estimatedDeliveryTime;
    private String deliveryInstructions;

    // Cancellation info
    private String cancellationReason;
    private String cancellationNote;
    private LocalDateTime cancelledAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
