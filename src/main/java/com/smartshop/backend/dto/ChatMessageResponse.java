package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private boolean success;
    private String reply;
    private String intent;
    private String conversationId;
    private List<Map<String, String>> actions; // [{label: "View Order", url: "/orders"}, ...]
    private List<ProductDTO> products;          // for product search results
    private List<OrderDTO> orders;              // for order status results
    private CartDTO cart;                       // for cart view
}
