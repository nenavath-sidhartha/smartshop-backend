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
public class ChatConversationDTO {
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String sessionId;
    private String status;
    private Long assignedAdminId;
    private String assignedAdminName;
    private List<ChatMessageDTO> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Summary info (without full messages list)
    private String lastMessage;
    private int messageCount;
}
