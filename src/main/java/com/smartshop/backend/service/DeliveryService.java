package com.smartshop.backend.service;

import com.smartshop.backend.dto.*;

import java.util.List;

public interface DeliveryService {

    // --- Delivery Agent Operations ---
    List<DeliveryAssignmentDTO> getAssignedOrdersForAgent(String agentEmail);

    DeliveryAssignmentDTO getAgentOrderDetails(Long orderId, String agentEmail);

    DeliveryAssignmentDTO startDelivery(Long orderId, String agentEmail);

    DeliveryLocationDTO updateLocation(Long orderId, String agentEmail, DeliveryLocationRequest request);

    DeliveryAssignmentDTO completeDelivery(Long orderId, String agentEmail);

    // --- Admin Operations ---
    List<OrderDTO> getUnassignedOrders();

    List<DeliveryAgentDTO> getAvailableDeliveryAgents();

    List<DeliveryAssignmentDTO> getAllAssignments();

    DeliveryAssignmentDTO assignDeliveryAgent(AssignDeliveryRequest request);

    DeliveryAssignmentDTO reassignDeliveryAgent(AssignDeliveryRequest request);
}
