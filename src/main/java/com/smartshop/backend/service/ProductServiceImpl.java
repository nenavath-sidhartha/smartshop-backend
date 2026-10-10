package com.smartshop.backend.service;

import com.smartshop.backend.dto.ProductDTO;
import com.smartshop.backend.dto.ProductRequest;
import com.smartshop.backend.entity.Category;
import com.smartshop.backend.entity.Product;
import com.smartshop.backend.exception.ResourceNotFoundException;
import com.smartshop.backend.repository.CategoryRepository;
import com.smartshop.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public ProductDTO createProduct(ProductRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice() != null ? request.getOriginalPrice() : request.getPrice())
                .discount(request.getDiscount() != null ? request.getDiscount() : 0)
                .stock(request.getStock())
                .brand(request.getBrand())
                .category(category)
                .image(request.getImage())
                .rating(request.getRating() != null ? request.getRating() : 4.5)
                .reviewCount(request.getReviewCount() != null ? request.getReviewCount() : 0)
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        if (request.getOriginalPrice() != null) {
            product.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getDiscount() != null) {
            product.setDiscount(request.getDiscount());
        }
        product.setStock(request.getStock());
        product.setBrand(request.getBrand());
        if (request.getImage() != null) {
            product.setImage(request.getImage());
        }
        if (request.getRating() != null) {
            product.setRating(request.getRating());
        }
        if (request.getReviewCount() != null) {
            product.setReviewCount(request.getReviewCount());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapToDTO(product);
    }

    @Override
    public List<ProductDTO> getAllProducts(String search, Long categoryId, String brand, Double minPrice, Double maxPrice, String sort) {
        List<Product> products = productRepository.filterProducts(search, categoryId, brand, minPrice, maxPrice);

        if (sort != null) {
            switch (sort.toLowerCase()) {
                case "priceasc":
                    products.sort(Comparator.comparing(Product::getPrice));
                    break;
                case "pricedesc":
                    products.sort(Comparator.comparing(Product::getPrice).reversed());
                    break;
                case "popularity":
                case "rating":
                    products.sort(Comparator.comparing(Product::getRating).reversed());
                    break;
                case "newest":
                default:
                    products.sort(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                    break;
            }
        }

        return products.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productRepository.delete(product);
    }

    @Override
    public boolean checkStock(Long id, Integer requiredQuantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return product.getStock() >= requiredQuantity;
    }

    private ProductDTO mapToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discount(product.getDiscount())
                .stock(product.getStock())
                .brand(product.getBrand())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .image(product.getImage())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
