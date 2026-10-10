package com.smartshop.backend.controller;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // ─── REORDER & RECENT ENDPOINTS ──────────────────────────────────────────

    /**
     * GET /api/orders/my/recent
     * Retrieves recent orders for the authenticated customer.
     */
    @GetMapping("/my/recent")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getMyRecentOrders(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String email
    ) {
        String userEmail = resolveUserEmail(email);
        List<OrderDTO> recentOrders = orderService.getRecentOrdersForUser(userEmail, limit);
        return ResponseEntity.ok(ApiResponse.success("Recent orders retrieved successfully", recentOrders));
    }

    /**
     * GET /api/orders/my/recent-products
     * Retrieves distinct products previously purchased by the authenticated customer.
     */
    @GetMapping("/my/recent-products")
    public ResponseEntity<ApiResponse<List<RecentPurchasedProductDTO>>> getMyRecentPurchasedProducts(
            @RequestParam(required = false) String email
    ) {
        String userEmail = resolveUserEmail(email);
        List<RecentPurchasedProductDTO> products = orderService.getRecentPurchasedProducts(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Recent purchased products retrieved successfully", products));
    }

    /**
     * GET /api/orders/{orderId}/reorder-summary
     * Validates product availability, stock, and price changes without modifying cart.
     */
    @GetMapping("/{orderId}/reorder-summary")
    public ResponseEntity<ApiResponse<ReorderSummaryResponse>> getReorderSummary(
            @PathVariable Long orderId,
            @RequestParam(required = false) String email
    ) {
        String userEmail = resolveUserEmail(email);
        ReorderSummaryResponse summary = orderService.getReorderSummary(orderId, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Reorder summary prepared successfully", summary));
    }

    /**
     * POST /api/orders/{orderId}/reorder
     * Adds eligible products from the previous order to the cart with current prices, preserving existing items.
     */
    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<ApiResponse<ReorderSummaryResponse>> reorder(
            @PathVariable Long orderId,
            @RequestBody(required = false) ReorderExecuteRequest request,
            @RequestParam(required = false) String email
    ) {
        String userEmail = resolveUserEmail(email);
        ReorderSummaryResponse result = orderService.executeReorder(orderId, userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Products added to cart for reorder", result));
    }

    // ─── ORDER DELIVERY TRACKING ENDPOINTS ───────────────────────────────────

    /**
     * GET /api/orders/{orderId}/tracking
     * Comprehensive tracking info: status timeline, agent details, destination, and latest location.
     */
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingDTO>> getOrderTracking(
            @PathVariable Long orderId,
            @RequestParam(required = false) String email
    ) {
        String userEmail = resolveUserEmail(email);
        OrderTrackingDTO tracking = orderService.getOrderTracking(orderId, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Order tracking details fetched successfully", tracking));
    }

    /**
     * GET /api/orders/{orderId}/tracking/location
     * Lightweight endpoint for polling the delivery agent's latest coordinates.
     */
    @GetMapping("/{orderId}/tracking/location")
    public ResponseEntity<ApiResponse<DeliveryLocationDTO>> getLatestTrackingLocation(
            @PathVariable Long orderId,
            @RequestParam(required = false) String email
    ) {
        String userEmail = resolveUserEmail(email);
        DeliveryLocationDTO location = orderService.getLatestTrackingLocation(orderId, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Latest delivery location retrieved", location));
    }

    private String resolveUserEmail(String fallbackEmail) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        if (fallbackEmail != null && !fallbackEmail.isBlank()) {
            return fallbackEmail;
        }
        return "customer@smartshop.com";
    }
}
