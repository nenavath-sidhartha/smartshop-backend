package com.smartshop.backend.repository;

import com.smartshop.backend.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<ChatConversation> findBySessionIdOrderByUpdatedAtDesc(String sessionId);
    Optional<ChatConversation> findBySessionIdAndStatus(String sessionId, String status);
    List<ChatConversation> findByStatusOrderByUpdatedAtDesc(String status);
    List<ChatConversation> findAllByOrderByUpdatedAtDesc();
}
