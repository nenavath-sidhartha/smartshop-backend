package com.smartshop.backend.service;

import com.smartshop.backend.dto.CategoryDTO;
import com.smartshop.backend.dto.CategoryRequest;

import java.util.List;

public interface CategoryService {
    CategoryDTO createCategory(CategoryRequest request);
    CategoryDTO updateCategory(Long id, CategoryRequest request);
    CategoryDTO getCategoryById(Long id);
    List<CategoryDTO> getAllCategories();
    void deleteCategory(Long id);
}
