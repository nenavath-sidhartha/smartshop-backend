package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {
    private String message;
    private String conversationId; // UUID string for session tracking
    private String sessionId;      // Guest session ID (for anonymous users)
}
