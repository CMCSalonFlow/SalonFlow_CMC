package com.example.salonflow.services.service;

import com.example.salonflow.dto.category.CategoryResponse;
import com.example.salonflow.dto.category.CreateCategoryRequest;
import com.example.salonflow.dto.category.UpdateCategoryRequest;

import java.util.List;

public interface ServiceCategoryService {

    CategoryResponse create(CreateCategoryRequest request);

    List<CategoryResponse> getAll();

    CategoryResponse getById(Long id);

    CategoryResponse update(Long id, UpdateCategoryRequest request);

    void delete(Long id);

    // Thêm mới cho Drag & Drop
    void updateOrder(List<Long> orderedIds);
}