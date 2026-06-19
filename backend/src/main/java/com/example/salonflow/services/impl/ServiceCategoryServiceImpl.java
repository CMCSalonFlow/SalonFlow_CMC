package com.example.salonflow.services.impl;

import com.example.salonflow.dto.category.CategoryResponse;
import com.example.salonflow.dto.category.CreateCategoryRequest;
import com.example.salonflow.dto.category.UpdateCategoryRequest;
import com.example.salonflow.entity.ServiceCategory;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.ServiceCategoryRepository;
import com.example.salonflow.services.service.ServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCategoryServiceImpl implements ServiceCategoryService {

    private final ServiceCategoryRepository repository;

    @Override
    public CategoryResponse create(CreateCategoryRequest request) {
        Integer maxOrder = repository.findMaxDisplayOrder();
        int newOrder = (maxOrder != null) ? maxOrder + 1 : 0;

        ServiceCategory category = ServiceCategory.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .color(request.getColor())
                .description(request.getDescription())
                .displayOrder(newOrder)
                .build();

        category = repository.save(category);

        return toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return repository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getById(Long id) {
        ServiceCategory category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        return toResponse(category);
    }

    @Override
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        ServiceCategory category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setDescription(request.getDescription());

        category = repository.save(category);

        return toResponse(category);
    }

    @Override
    public void delete(Long id) {
        ServiceCategory category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        repository.delete(category);
    }

    // === DRAG & DROP ===
    @Override
    public void updateOrder(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }

        List<ServiceCategory> categories = repository.findAllById(orderedIds);

        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            ServiceCategory category = categories.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

            category.setDisplayOrder(i);
        }

        repository.saveAll(categories);
    }

    // Helper method
    private CategoryResponse toResponse(ServiceCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .build();
    }
}