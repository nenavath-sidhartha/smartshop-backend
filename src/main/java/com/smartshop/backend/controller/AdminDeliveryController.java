package com.smartshop.backend.controller;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/deliveries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    /**
     * GET /api/admin/deliveries/unassigned
     * Orders placed/confirmed that require a delivery agent assignment.
     */
    @GetMapping("/unassigned")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getUnassignedOrders() {
        List<OrderDTO> orders = deliveryService.getUnassignedOrders();
        return ResponseEntity.ok(ApiResponse.success("Unassigned orders retrieved", orders));
    }

    /**
     * GET /api/admin/deliveries/agents
     * Available delivery agents.
     */
    @GetMapping("/agents")
    public ResponseEntity<ApiResponse<List<DeliveryAgentDTO>>> getDeliveryAgents() {
        List<DeliveryAgentDTO> agents = deliveryService.getAvailableDeliveryAgents();
        return ResponseEntity.ok(ApiResponse.success("Delivery agents retrieved", agents));
    }

    /**
     * GET /api/admin/deliveries/assignments
     * All delivery assignments across all orders.
     */
    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentDTO>>> getAllAssignments() {
        List<DeliveryAssignmentDTO> assignments = deliveryService.getAllAssignments();
        return ResponseEntity.ok(ApiResponse.success("All delivery assignments retrieved", assignments));
    }

    /**
     * POST /api/admin/deliveries/assign
     * Assigns a delivery agent to an order.
     */
    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<DeliveryAssignmentDTO>> assignAgent(
            @Valid @RequestBody AssignDeliveryRequest request
    ) {
        DeliveryAssignmentDTO assignment = deliveryService.assignDeliveryAgent(request);
        return ResponseEntity.ok(ApiResponse.success("Delivery agent assigned successfully", assignment));
    }

    /**
     * POST /api/admin/deliveries/reassign
     * Reassigns an order to another delivery agent.
     */
    @PostMapping("/reassign")
    public ResponseEntity<ApiResponse<DeliveryAssignmentDTO>> reassignAgent(
            @Valid @RequestBody AssignDeliveryRequest request
    ) {
        DeliveryAssignmentDTO assignment = deliveryService.reassignDeliveryAgent(request);
        return ResponseEntity.ok(ApiResponse.success("Delivery agent reassigned successfully", assignment));
    }
}
