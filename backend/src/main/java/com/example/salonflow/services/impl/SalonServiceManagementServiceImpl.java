package com.example.salonflow.services.impl;

import com.example.salonflow.dto.service.CreateServiceRequest;
import com.example.salonflow.dto.service.ServiceResponse;
import com.example.salonflow.dto.service.UpdateServiceRequest;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.SalonService;
import com.example.salonflow.entity.ServiceCategory;
import com.example.salonflow.entity.ServiceImage;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ServiceCategoryRepository;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.search.service.BranchSearchService;
import com.example.salonflow.services.service.ServiceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import com.example.salonflow.validation.ServiceValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * ⚠️ Dùng @org.springframework.stereotype.Service (fully-qualified) thay vì
 * import + @Service, vì entity com.example.salonflow.entity.Service đã
 * chiếm tên "Service" trong file này — import cả hai sẽ bị lỗi biên dịch
 * "duplicate class: Service".
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SalonServiceManagementServiceImpl implements ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final BranchRepository branchRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceValidator serviceValidator;
    private final BranchSearchService branchSearchService;

    @Override
    @Transactional
    public ServiceResponse create(
            Long branchId,
            CreateServiceRequest request
    ) {
        serviceValidator.validateDeposit(
                request.getDepositRequired(),
                request.getDepositPercentage()
        );
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch with id " + branchId + " not found"));

        ServiceCategory category = resolveCategory(request.getCategoryId());

        SalonService service = SalonService.builder()
                .branch(branch)
                .category(category)
                .name(request.getName())
                .price(request.getPrice())
                .durationMinutes(request.getDurationMinutes())
                .description(request.getDescription())
                .depositRequired(Boolean.TRUE.equals(request.getDepositRequired()))
                .depositPercentage(
                        Boolean.TRUE.equals(request.getDepositRequired())
                                ? request.getDepositPercentage()
                                : null
                )
                .isActive(true)
                .build();

        attachImages(service, request.getImages());

        service = serviceRepository.save(service);
        
        branchSearchService.indexBranch(branchId);

        return toResponse(service);
    }

    @Override
    public List<ServiceResponse> getByBranch(Long branchId) {

        ensureBranchExists(branchId);

        return serviceRepository.findByBranchId(branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ServiceResponse> getByBranchActiveOnly(Long branchId) {

        ensureBranchExists(branchId);

        return serviceRepository.findByBranchIdAndIsActiveTrue(branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ServiceResponse getById(Long branchId, Long serviceId) {

        SalonService service = findOwnedService(branchId, serviceId);

        return toResponse(service);
    }

    @Override
    @Transactional
    public ServiceResponse update(
            Long branchId,
            Long serviceId,
            UpdateServiceRequest request
    ) {

        SalonService service = findOwnedService(branchId, serviceId);

        ServiceCategory category = resolveCategory(request.getCategoryId());

        serviceValidator.validateDeposit(
                request.getDepositRequired(),
                request.getDepositPercentage()
        );
        service.setName(request.getName());
        service.setCategory(category);
        service.setPrice(request.getPrice());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setDescription(request.getDescription());

        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        if (request.getImages() != null) {
            service.getImages().clear();
            attachImages(service, request.getImages());
        }

        service.setDepositRequired(
                Boolean.TRUE.equals(request.getDepositRequired())
        );

        service.setDepositPercentage(
                Boolean.TRUE.equals(request.getDepositRequired())
                        ? request.getDepositPercentage()
                        : null
        );
        service = serviceRepository.save(service);

        branchSearchService.indexBranch(branchId);

        return toResponse(service);
    }

    @Override
    @Transactional
    public void delete(Long branchId, Long serviceId) {

        SalonService service = findOwnedService(branchId, serviceId);

        serviceRepository.delete(service);
        branchSearchService.indexBranch(branchId);
    }

    // ── Helpers ────────────────────────────────────────────────

    private void ensureBranchExists(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException(
                    "Branch with id " + branchId + " not found");
        }
    }

    private SalonService findOwnedService(Long branchId, Long serviceId) {
        return serviceRepository.findByIdAndBranchId(serviceId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service with id " + serviceId
                                + " not found in branch " + branchId));
    }

    private ServiceCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with id " + categoryId + " not found"));
    }

    private void attachImages(SalonService service, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<ServiceImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(ServiceImage.builder()
                    .service(service)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build());
        }
        service.getImages().addAll(images);
    }

    private ServiceResponse toResponse(SalonService service) {

        List<String> imageUrls = service.getImages().stream()
                .sorted((a, b) -> {
                    Integer oa = a.getDisplayOrder() == null ? 0 : a.getDisplayOrder();
                    Integer ob = b.getDisplayOrder() == null ? 0 : b.getDisplayOrder();
                    return oa.compareTo(ob);
                })
                .map(ServiceImage::getImageUrl)
                .toList();

        return ServiceResponse.builder()
                .id(service.getId())
                .branchId(service.getBranch().getId())
                .categoryId(service.getCategory() != null
                        ? service.getCategory().getId() : null)
                .categoryName(service.getCategory() != null
                        ? service.getCategory().getName() : null)
                .name(service.getName())
                .price(service.getPrice())
                .durationMinutes(service.getDurationMinutes())
                .description(service.getDescription())
                .depositRequired(service.getDepositRequired())
                .depositPercentage(service.getDepositPercentage())
                .isActive(service.getIsActive())
                .images(imageUrls)
                .build();
    }
}
