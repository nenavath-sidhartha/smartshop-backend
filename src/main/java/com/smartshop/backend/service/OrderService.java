package com.smartshop.backend.service;

import com.smartshop.backend.dto.*;

import java.util.List;

public interface OrderService {
    OrderDTO placeOrder(PlaceOrderRequest request);
    OrderDTO getOrderById(Long orderId);
    OrderDTO getOrderByOrderNumber(String orderNumber);
    List<OrderDTO> getOrdersByUserId(Long userId, String status);
    List<OrderDTO> getAllOrders(String status);
    OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);
    DashboardStatsDTO getAdminDashboardStats();

    /** Cancel an order on behalf of a customer. Validates ownership, status, restores inventory. */
    OrderDTO cancelOrder(Long orderId, CancelOrderRequest request);

    /** Retrieve recent orders for the authenticated customer */
    List<OrderDTO> getRecentOrdersForUser(String userEmail, int limit);

    /** Retrieve distinct recently purchased products for the authenticated customer */
    List<RecentPurchasedProductDTO> getRecentPurchasedProducts(String userEmail);

    /** Prepare reorder summary checking current price and stock without altering cart */
    ReorderSummaryResponse getReorderSummary(Long orderId, String userEmail);

    /** Add eligible reorder items to the customer cart, preserving existing items */
    ReorderSummaryResponse executeReorder(Long orderId, String userEmail, ReorderExecuteRequest request);

    /** Retrieve comprehensive delivery tracking details for an order */
    OrderTrackingDTO getOrderTracking(Long orderId, String userEmail);

    /** Retrieve only latest delivery location coordinates for live polling */
    DeliveryLocationDTO getLatestTrackingLocation(Long orderId, String userEmail);
}
