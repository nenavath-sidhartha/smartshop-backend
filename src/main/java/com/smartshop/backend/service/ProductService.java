package com.smartshop.backend.service;

import com.smartshop.backend.dto.ProductDTO;
import com.smartshop.backend.dto.ProductRequest;

import java.util.List;

public interface ProductService {
    ProductDTO createProduct(ProductRequest request);
    ProductDTO updateProduct(Long id, ProductRequest request);
    ProductDTO getProductById(Long id);
    List<ProductDTO> getAllProducts(String search, Long categoryId, String brand, Double minPrice, Double maxPrice, String sort);
    void deleteProduct(Long id);
    boolean checkStock(Long id, Integer requiredQuantity);
}
