package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderSummaryResponse {
    private Long orderId;
    private String orderNumber;
    @Builder.Default
    private List<ReorderItemDTO> items = new ArrayList<>();
    private Boolean allItemsAvailable;
    private Boolean hasPriceChanges;
    private String priceChangeMessage;
    private Integer unavailableItemsCount;
    private Double eligibleSubtotal;
    private CartDTO updatedCart;
}
