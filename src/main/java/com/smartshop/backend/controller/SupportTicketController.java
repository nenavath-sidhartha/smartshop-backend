package com.smartshop.backend.controller;

import com.smartshop.backend.dto.SupportDashboardDTO;
import com.smartshop.backend.dto.SupportTicketRequest;
import com.smartshop.backend.dto.SupportTicketResponse;
import com.smartshop.backend.dto.TicketMessageRequest;
import com.smartshop.backend.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    // ─────────────── CUSTOMER ENDPOINTS ───────────────

    /** Create a new support ticket (authenticated) */
    @PostMapping("/tickets")
    public ResponseEntity<SupportTicketResponse> createTicket(
            @Valid @RequestBody SupportTicketRequest request,
            Principal principal) {
        SupportTicketResponse response = supportTicketService.createTicket(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Get current user's tickets */
    @GetMapping("/tickets/my")
    public ResponseEntity<List<SupportTicketResponse>> getMyTickets(Principal principal) {
        return ResponseEntity.ok(supportTicketService.getMyTickets(principal.getName()));
    }

    /** Get a specific ticket belonging to the current user */
    @GetMapping("/tickets/my/{id}")
    public ResponseEntity<SupportTicketResponse> getMyTicket(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(supportTicketService.getMyTicketById(id, principal.getName()));
    }

    /** Reply to a ticket (customer) */
    @PostMapping("/tickets/my/{id}/messages")
    public ResponseEntity<SupportTicketResponse> addCustomerMessage(
            @PathVariable Long id,
            @RequestBody TicketMessageRequest request,
            Principal principal) {
        return ResponseEntity.ok(supportTicketService.addCustomerMessage(id, request, principal.getName()));
    }

    // ─────────────── ADMIN ENDPOINTS ───────────────

    /** ADMIN: Get all tickets, optionally filter by status */
    @GetMapping("/admin/tickets")
    public ResponseEntity<List<SupportTicketResponse>> getAllTickets(
            @RequestParam(defaultValue = "ALL") String status) {
        return ResponseEntity.ok(supportTicketService.getAllTickets(status));
    }

    /** ADMIN: Get a single ticket by id */
    @GetMapping("/admin/tickets/{id}")
    public ResponseEntity<SupportTicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(supportTicketService.getTicketById(id));
    }

    /** ADMIN: Update ticket status and/or assign to admin */
    @PatchMapping("/admin/tickets/{id}")
    public ResponseEntity<SupportTicketResponse> updateTicket(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assignedToId) {
        return ResponseEntity.ok(supportTicketService.updateTicket(id, status, assignedToId));
    }

    /** ADMIN: Reply to a ticket */
    @PostMapping("/admin/tickets/{id}/messages")
    public ResponseEntity<SupportTicketResponse> addAdminMessage(
            @PathVariable Long id,
            @RequestBody TicketMessageRequest request,
            Principal principal) {
        return ResponseEntity.ok(supportTicketService.addAdminMessage(id, request, principal.getName()));
    }

    /** ADMIN: Delete ticket */
    @DeleteMapping("/admin/tickets/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        supportTicketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    /** ADMIN: Support dashboard stats */
    @GetMapping("/admin/dashboard")
    public ResponseEntity<SupportDashboardDTO> getDashboardStats() {
        return ResponseEntity.ok(supportTicketService.getDashboardStats());
    }
}
