package com.smartshop.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketRequest {
    @NotBlank(message = "Subject is required")
    @Size(max = 300, message = "Subject must be under 300 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    private String category;  // ORDER, PAYMENT, DELIVERY, RETURN, REFUND, PRODUCT, ACCOUNT, OTHER
    private String priority;  // LOW, MEDIUM, HIGH, URGENT
    private String relatedOrderNumber;
}
