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
}
