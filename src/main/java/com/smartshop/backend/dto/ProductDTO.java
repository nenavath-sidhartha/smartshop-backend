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
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double originalPrice;
    private Integer discount;
    private Integer stock;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private String image;
    private Double rating;
    private Integer reviewCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
