package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketResponse {
    private Long id;
    private String ticketNumber;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String subject;
    private String description;
    private String category;
    private String priority;
    private String status;
    private Long assignedToId;
    private String assignedToName;
    private String relatedOrderNumber;
    private List<TicketMessageDTO> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
