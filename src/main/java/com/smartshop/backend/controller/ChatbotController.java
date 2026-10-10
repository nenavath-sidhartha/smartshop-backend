package com.smartshop.backend.controller;

import com.smartshop.backend.dto.ChatConversationDTO;
import com.smartshop.backend.dto.ChatMessageRequest;
import com.smartshop.backend.dto.ChatMessageResponse;
import com.smartshop.backend.repository.UserRepository;
import com.smartshop.backend.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final UserRepository userRepository;

    /** Public / authenticated: process a chat message */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @RequestBody ChatMessageRequest request,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        ChatMessageResponse response = chatbotService.processMessage(request, email);
        return ResponseEntity.ok(response);
    }

    /** Get conversation history */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ChatConversationDTO> getConversation(
            @PathVariable Long conversationId,
            Principal principal) {
        Long userId = null;
        if (principal != null) {
            userId = userRepository.findByEmail(principal.getName())
                    .map(u -> u.getId()).orElse(null);
        }
        ChatConversationDTO dto = chatbotService.getConversationById(conversationId, userId);
        return ResponseEntity.ok(dto);
    }

    /** ADMIN: get all conversations, optionally filtered by status */
    @GetMapping("/admin/conversations")
    public ResponseEntity<List<ChatConversationDTO>> getAllConversations() {
        return ResponseEntity.ok(chatbotService.getAllConversations());
    }

    /** ADMIN: join / assign self to a conversation */
    @PostMapping("/admin/conversations/{conversationId}/join")
    public ResponseEntity<ChatConversationDTO> joinConversation(
            @PathVariable Long conversationId,
            Principal principal) {
        ChatConversationDTO dto = chatbotService.adminJoinConversation(conversationId, principal.getName());
        return ResponseEntity.ok(dto);
    }

    /** ADMIN: send message in conversation */
    @PostMapping("/admin/conversations/{conversationId}/message")
    public ResponseEntity<ChatConversationDTO> adminSendMessage(
            @PathVariable Long conversationId,
            @RequestBody ChatMessageRequest request,
            Principal principal) {
        ChatConversationDTO dto = chatbotService.adminSendMessage(conversationId, request.getMessage(), principal.getName());
        return ResponseEntity.ok(dto);
    }

    /** User: get own conversations */
    @GetMapping("/my-conversations")
    public ResponseEntity<List<ChatConversationDTO>> getMyConversations(Principal principal) {
        Long userId = userRepository.findByEmail(principal.getName())
                .map(u -> u.getId()).orElse(null);
        if (userId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(chatbotService.getConversationsForUser(userId));
    }
}
