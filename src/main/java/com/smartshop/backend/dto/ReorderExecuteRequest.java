package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderExecuteRequest {
    /**
     * Optional map of productId -> customized quantity to add to cart.
     * If null or empty, the quantities from the original order are used (bounded by available stock).
     */
    private Map<Long, Integer> itemQuantities;
}
