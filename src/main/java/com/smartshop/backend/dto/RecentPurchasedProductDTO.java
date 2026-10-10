package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentPurchasedProductDTO {
    private Long productId;
    private String productName;
    private String productImage;
    private String brand;
    private String categoryName;
    private Double currentPrice;
    private Double previousPrice;
    private Boolean priceChanged;
    private LocalDateTime previousPurchaseDate;
    private Integer previousQuantity;
    private Integer stock;
    private Boolean inStock;
    private Double rating;
    private Integer reviewCount;
    private String previousOrderNumber;
    private Long previousOrderId;
}
