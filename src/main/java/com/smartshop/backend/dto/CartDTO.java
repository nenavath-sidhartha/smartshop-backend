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
public class CartDTO {
    private Long id;
    private Long userId;
    @Builder.Default
    private List<CartItemDTO> items = new ArrayList<>();
    private Double subtotal;
    private Double deliveryFee;
    private Double total;
    private Integer totalItemsCount;
}
