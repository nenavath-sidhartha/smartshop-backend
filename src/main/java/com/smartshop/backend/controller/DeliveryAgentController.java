package com.smartshop.backend.controller;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeliveryAgentController {

    private final DeliveryService deliveryService;

    /**
     * GET /api/delivery/my-orders
     * Retrieves all orders assigned to the authenticated delivery agent.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentDTO>>> getMyAssignedOrders(
            @RequestParam(required = false) String email
    ) {
        String agentEmail = resolveAgentEmail(email);
        List<DeliveryAssignmentDTO> orders = deliveryService.getAssignedOrdersForAgent(agentEmail);
        return ResponseEntity.ok(ApiResponse.success("Assigned delivery orders retrieved", orders));
    }

    /**
     * GET /api/delivery/orders/{orderId}
     * Retrieves full delivery details for a specific order.
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<DeliveryAssignmentDTO>> getOrderDetails(
            @PathVariable Long orderId,
            @RequestParam(required = false) String email
    ) {
        String agentEmail = resolveAgentEmail(email);
        DeliveryAssignmentDTO assignment = deliveryService.getAgentOrderDetails(orderId, agentEmail);
        return ResponseEntity.ok(ApiResponse.success("Order delivery details retrieved", assignment));
    }

    /**
     * POST /api/delivery/orders/{orderId}/start
     * Agent starts the delivery run -> status becomes OUT_FOR_DELIVERY.
     */
    @PostMapping("/orders/{orderId}/start")
    public ResponseEntity<ApiResponse<DeliveryAssignmentDTO>> startDelivery(
            @PathVariable Long orderId,
            @RequestParam(required = false) String email
    ) {
        String agentEmail = resolveAgentEmail(email);
        DeliveryAssignmentDTO result = deliveryService.startDelivery(orderId, agentEmail);
        return ResponseEntity.ok(ApiResponse.success("Delivery started successfully. Live tracking is active.", result));
    }

    /**
     * POST /api/delivery/orders/{orderId}/location
     * Accepts live GPS updates from delivery agent's device.
     */
    @PostMapping("/orders/{orderId}/location")
    public ResponseEntity<ApiResponse<DeliveryLocationDTO>> updateLocation(
            @PathVariable Long orderId,
            @Valid @RequestBody DeliveryLocationRequest request,
            @RequestParam(required = false) String email
    ) {
        String agentEmail = resolveAgentEmail(email);
        DeliveryLocationDTO loc = deliveryService.updateLocation(orderId, agentEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", loc));
    }

    /**
     * POST /api/delivery/orders/{orderId}/complete
     * Marks order as DELIVERED and stops active location tracking.
     */
    @PostMapping("/orders/{orderId}/complete")
    public ResponseEntity<ApiResponse<DeliveryAssignmentDTO>> completeDelivery(
            @PathVariable Long orderId,
            @RequestParam(required = false) String email
    ) {
        String agentEmail = resolveAgentEmail(email);
        DeliveryAssignmentDTO result = deliveryService.completeDelivery(orderId, agentEmail);
        return ResponseEntity.ok(ApiResponse.success("Order marked as DELIVERED successfully", result));
    }

    private String resolveAgentEmail(String fallbackEmail) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        if (fallbackEmail != null && !fallbackEmail.isBlank()) {
            return fallbackEmail;
        }
        return "agent@smartshop.com";
    }
}
