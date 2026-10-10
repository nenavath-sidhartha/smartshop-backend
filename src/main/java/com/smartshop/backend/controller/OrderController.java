package com.smartshop.backend.controller;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        OrderDTO createdOrder = orderService.placeOrder(request);
        return new ResponseEntity<>(ApiResponse.success("Order placed successfully!", createdOrder), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getAllOrders(@RequestParam(required = false) String status) {
        List<OrderDTO> orders = orderService.getAllOrders(status);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", order));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderDTO order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order details fetched", order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) String status
    ) {
        List<OrderDTO> orders = orderService.getOrdersByUserId(userId, status);
        return ResponseEntity.ok(ApiResponse.success("User orders retrieved", orders));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderDTO updatedOrder = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", updatedOrder));
    }

    /**
     * Customer-initiated order cancellation.
     * PATCH /api/orders/{orderId}/cancel
     * The body must include userId for ownership validation.
     * Backend enforces: ownership check, cancellable-status check, inventory restoration.
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody CancelOrderRequest request
    ) {
        OrderDTO cancelled = orderService.cancelOrder(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", cancelled));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getAdminDashboardStats() {
        DashboardStatsDTO stats = orderService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard statistics retrieved", stats));
    }
}
