package com.smartshop.backend.repository;

import com.smartshop.backend.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);
    Optional<SupportTicket> findByIdAndUserId(Long id, Long userId);
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(String status);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
    long countByStatusAndResolvedAtAfter(String status, LocalDateTime after);
}
