package com.example.salonflow.services.impl;

import com.example.salonflow.dto.category.CategoryResponse;
import com.example.salonflow.dto.category.CreateCategoryRequest;
import com.example.salonflow.dto.category.UpdateCategoryRequest;
import com.example.salonflow.entity.MediaFile;
import com.example.salonflow.entity.ServiceCategory;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.MediaFileRepository;
import com.example.salonflow.repository.ServiceCategoryRepository;
import com.example.salonflow.services.service.ServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCategoryServiceImpl implements ServiceCategoryService {

    private final MediaFileRepository mediaRepository;
    private final ServiceCategoryRepository repository;

    @Override
    public CategoryResponse create(CreateCategoryRequest request) {
        if (request.getName() != null && repository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new BadRequestException("Tên danh mục '" + request.getName().trim() + "' đã tồn tại trong hệ thống. Vui lòng chọn tên khác!");
        }

        Integer maxOrder = repository.findMaxDisplayOrder();
        int newOrder = (maxOrder != null) ? maxOrder + 1 : 0;

        MediaFile icon = null;

        if (request.getIconMediaId() != null) {
            icon = mediaRepository.findById(request.getIconMediaId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Icon media không tồn tại"));
        }

        ServiceCategory category = ServiceCategory.builder()
                .name(request.getName().trim())
                .icon(icon)
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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getName() != null && repository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new BadRequestException("Tên danh mục '" + request.getName().trim() + "' đã tồn tại trong hệ thống. Vui lòng chọn tên khác!");
        }

        MediaFile icon = null;

        if (request.getIconMediaId() != null) {
            icon = mediaRepository.findById(request.getIconMediaId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Icon media không tồn tại"));
        }

        category.setName(request.getName().trim());
        category.setIcon(icon);
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

    private CategoryResponse toResponse(ServiceCategory category) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .iconUrl(category.getIcon() != null
                        ? category.getIcon().getUrl()
                        : null)
                .color(category.getColor())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .build();
    }
}