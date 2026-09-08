package com.example.salonflow.services.impl;

import com.example.salonflow.dto.category.CategoryResponse;
import com.example.salonflow.dto.category.CreateCategoryRequest;
import com.example.salonflow.dto.category.UpdateCategoryRequest;
import com.example.salonflow.entity.MediaFile;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.ServiceCategory;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.MediaFileRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.repository.ServiceCategoryRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.ServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceCategoryServiceImpl implements ServiceCategoryService {

    private final MediaFileRepository mediaRepository;
    private final ServiceCategoryRepository repository;
    private final SalonRepository salonRepository;

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private Optional<Long> getScopedSalonId() {
        if (isAdmin()) {
            return Optional.empty(); // Super admin xem toàn hệ thống
        }
        Optional<Long> currentUserIdOpt = SecurityUtils.getCurrentUserIdOptional();
        if (currentUserIdOpt.isEmpty()) {
            return Optional.of(-1L);
        }
        return salonRepository.findFirstByOwnerId(currentUserIdOpt.get())
                .map(Salon::getId)
                .or(() -> Optional.of(-1L)); // User chưa tạo salon -> -1L
    }

    @Override
    public CategoryResponse create(CreateCategoryRequest request) {
        Long salonId = request.getSalonId();
        if (!isAdmin()) {
            Optional<Long> scopedSalonId = getScopedSalonId();
            if (scopedSalonId.isEmpty() || scopedSalonId.get().equals(-1L)) {
                throw new BadRequestException("Bạn chưa tạo salon! Vui lòng tạo thông tin salon trước khi tạo danh mục dịch vụ.");
            }
            salonId = scopedSalonId.get();
        }

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            boolean exists = (salonId != null)
                    ? repository.existsByNameIgnoreCaseAndSalonId(trimmedName, salonId)
                    : repository.existsByNameIgnoreCase(trimmedName);
            if (exists) {
                throw new BadRequestException("Tên danh mục '" + trimmedName + "' đã tồn tại trong salon của bạn. Vui lòng chọn tên khác!");
            }
        }

        Integer maxOrder = (salonId != null)
                ? repository.findMaxDisplayOrderBySalonId(salonId)
                : repository.findMaxDisplayOrder();
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
                .description(request.getDescription())
                .displayOrder(newOrder)
                .salonId(salonId)
                .build();

        category = repository.save(category);

        return toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return getAll(null);
    }

    @Override
    public List<CategoryResponse> getAll(Long requestedSalonId) {
        if (isAdmin()) {
            if (requestedSalonId != null) {
                return repository.findBySalonIdOrderByDisplayOrderAsc(requestedSalonId)
                        .stream().map(this::toResponse).toList();
            }
            return repository.findAllByOrderByDisplayOrderAsc()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        // Nếu client chỉ định salonId cụ thể (ví dụ khi khách xem salon storefront)
        if (requestedSalonId != null) {
            return repository.findBySalonIdOrderByDisplayOrderAsc(requestedSalonId)
                    .stream().map(this::toResponse).toList();
        }

        // Salon Owner đang login:
        Optional<Long> scopedSalonId = getScopedSalonId();
        if (scopedSalonId.isPresent()) {
            Long salonId = scopedSalonId.get();
            if (salonId.equals(-1L)) {
                // Chưa tạo salon -> Không có danh mục nào!
                return List.of();
            }
            return repository.findBySalonIdOrderByDisplayOrderAsc(salonId)
                    .stream().map(this::toResponse).toList();
        }

        return List.of();
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

        if (!isAdmin()) {
            Optional<Long> scopedSalonId = getScopedSalonId();
            Long ownerSalonId = scopedSalonId.orElse(-1L);
            if (category.getSalonId() != null && !category.getSalonId().equals(ownerSalonId)) {
                throw new BadRequestException("Bạn không có quyền chỉnh sửa danh mục của salon khác!");
            }
        }

        Long salonId = category.getSalonId();
        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            boolean exists = (salonId != null)
                    ? repository.existsByNameIgnoreCaseAndSalonIdAndIdNot(trimmedName, salonId, id)
                    : repository.existsByNameIgnoreCaseAndIdNot(trimmedName, id);
            if (exists) {
                throw new BadRequestException("Tên danh mục '" + trimmedName + "' đã tồn tại. Vui lòng chọn tên khác!");
            }
        }

        MediaFile icon = null;
        if (request.getIconMediaId() != null) {
            icon = mediaRepository.findById(request.getIconMediaId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Icon media không tồn tại"));
        }

        category.setName(request.getName().trim());
        category.setIcon(icon);
        category.setDescription(request.getDescription());

        category = repository.save(category);

        return toResponse(category);
    }

    @Override
    public void delete(Long id) {
        ServiceCategory category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!isAdmin()) {
            Optional<Long> scopedSalonId = getScopedSalonId();
            Long ownerSalonId = scopedSalonId.orElse(-1L);
            if (category.getSalonId() != null && !category.getSalonId().equals(ownerSalonId)) {
                throw new BadRequestException("Bạn không có quyền xóa danh mục của salon khác!");
            }
        }

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
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .salonId(category.getSalonId())
                .build();
    }
}