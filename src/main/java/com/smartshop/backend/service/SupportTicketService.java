package com.smartshop.backend.service;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.entity.*;
import com.smartshop.backend.exception.*;
import com.smartshop.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final UserRepository userRepository;
    private static final String TICKET_PREFIX = "SS-TKT-";
    private static final long TICKET_START = 10001L;

    // ─── CUSTOMER: Create Ticket ───
    @Transactional
    public SupportTicketResponse createTicket(SupportTicketRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        String ticketNumber = generateTicketNumber();

        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(ticketNumber)
                .user(user)
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : "OTHER")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .status("OPEN")
                .relatedOrderNumber(request.getRelatedOrderNumber())
                .build();

        SupportTicket saved = ticketRepository.save(ticket);

        // Add initial message from description
        TicketMessage initialMsg = TicketMessage.builder()
                .ticket(saved)
                .sender(user)
                .senderRole("CUSTOMER")
                .message(request.getDescription())
                .build();
        messageRepository.save(initialMsg);

        return toResponse(saved, List.of(initialMsg));
    }

    // ─── CUSTOMER: Get Own Tickets ───
    public List<SupportTicketResponse> getMyTickets(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(t -> toResponse(t, List.of())).collect(Collectors.toList());
    }

    // ─── CUSTOMER: Get Ticket Detail (own only) ───
    public SupportTicketResponse getMyTicketById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        SupportTicket ticket = ticketRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(id);
        return toResponse(ticket, messages);
    }

    // ─── CUSTOMER: Reply to Ticket ───
    @Transactional
    public SupportTicketResponse addCustomerMessage(Long id, TicketMessageRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        SupportTicket ticket = ticketRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        if ("CLOSED".equals(ticket.getStatus()) || "RESOLVED".equals(ticket.getStatus())) {
            throw new BadRequestException("Cannot reply to a closed or resolved ticket. Please create a new ticket.");
        }

        TicketMessage msg = TicketMessage.builder()
                .ticket(ticket)
                .sender(user)
                .senderRole("CUSTOMER")
                .message(request.getMessage())
                .build();
        messageRepository.save(msg);

        if ("WAITING_FOR_CUSTOMER".equals(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
            ticketRepository.save(ticket);
        }

        List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(id);
        return toResponse(ticket, messages);
    }

    // ─── ADMIN: Get All Tickets ───
    public List<SupportTicketResponse> getAllTickets(String status) {
        List<SupportTicket> tickets;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            tickets = ticketRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
        } else {
            tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        }
        return tickets.stream().map(t -> toResponse(t, List.of())).collect(Collectors.toList());
    }

    // ─── ADMIN: Get Any Ticket Detail ───
    public SupportTicketResponse getTicketById(Long id) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(id);
        return toResponse(ticket, messages);
    }

    // ─── ADMIN: Update Ticket Status / Assign ───
    @Transactional
    public SupportTicketResponse updateTicket(Long id, String status, Long assignedToId) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        if (status != null) {
            ticket.setStatus(status.toUpperCase());
            if ("RESOLVED".equals(status.toUpperCase()) || "CLOSED".equals(status.toUpperCase())) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }
        if (assignedToId != null) {
            User admin = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", assignedToId));
            ticket.setAssignedTo(admin);
        }
        ticketRepository.save(ticket);
        List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(id);
        return toResponse(ticket, messages);
    }

    // ─── ADMIN: Reply to Ticket ───
    @Transactional
    public SupportTicketResponse addAdminMessage(Long id, TicketMessageRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        TicketMessage msg = TicketMessage.builder()
                .ticket(ticket)
                .sender(admin)
                .senderRole("ADMIN")
                .message(request.getMessage())
                .build();
        messageRepository.save(msg);

        // Auto-move to IN_PROGRESS if OPEN
        if ("OPEN".equals(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
            ticketRepository.save(ticket);
        }

        List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(id);
        return toResponse(ticket, messages);
    }

    // ─── ADMIN: Delete Ticket ───
    @Transactional
    public void deleteTicket(Long id) {
        ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        ticketRepository.deleteById(id);
    }

    // ─── ADMIN: Dashboard Stats ───
    public SupportDashboardDTO getDashboardStats() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return SupportDashboardDTO.builder()
                .openTickets(ticketRepository.countByStatus("OPEN"))
                .inProgressTickets(ticketRepository.countByStatus("IN_PROGRESS"))
                .waitingForCustomer(ticketRepository.countByStatus("WAITING_FOR_CUSTOMER"))
                .resolvedToday(ticketRepository.countByStatusAndResolvedAtAfter("RESOLVED", startOfDay))
                .closedTickets(ticketRepository.countByStatus("CLOSED"))
                .totalTickets(ticketRepository.count())
                .build();
    }

    // ─── HELPERS ───

    private String generateTicketNumber() {
        long count = ticketRepository.count();
        long num = TICKET_START + count;
        return TICKET_PREFIX + num;
    }

    private SupportTicketResponse toResponse(SupportTicket t, List<TicketMessage> messages) {
        return SupportTicketResponse.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .userId(t.getUser().getId())
                .userFullName(t.getUser().getFullName())
                .userEmail(t.getUser().getEmail())
                .subject(t.getSubject())
                .description(t.getDescription())
                .category(t.getCategory())
                .priority(t.getPriority())
                .status(t.getStatus())
                .assignedToId(t.getAssignedTo() != null ? t.getAssignedTo().getId() : null)
                .assignedToName(t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null)
                .relatedOrderNumber(t.getRelatedOrderNumber())
                .messages(messages.stream().map(m -> TicketMessageDTO.builder()
                        .id(m.getId())
                        .ticketId(t.getId())
                        .senderId(m.getSender().getId())
                        .senderName(m.getSender().getFullName())
                        .senderRole(m.getSenderRole())
                        .message(m.getMessage())
                        .createdAt(m.getCreatedAt())
                        .build()).collect(Collectors.toList()))
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt())
                .build();
    }
}
