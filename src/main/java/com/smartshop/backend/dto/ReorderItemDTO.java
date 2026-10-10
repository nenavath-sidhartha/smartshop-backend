package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderItemDTO {
    private Long productId;
    private String productName;
    private String productImage;
    private String brand;
    private Double previousPrice;
    private Double currentPrice;
    private Boolean priceChanged;
    private Double priceDifference;
    private Integer quantity; // previous or adjusted quantity
    private Integer availableStock;
    private Boolean inStock;
    private Boolean discontinued;
    private Double rating;
}
