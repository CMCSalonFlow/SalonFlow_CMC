package com.example.salonflow.services.impl;

import com.example.salonflow.dto.bundle.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ServiceBundleRepository;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.services.service.ServiceBundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceBundleServiceImpl implements ServiceBundleService {

    private final ServiceBundleRepository serviceBundleRepository;
    private final ServiceRepository serviceRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public BundleResponse create(Long branchId, CreateBundleRequest request) {
        // 1. Ensure Branch exists
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch with id " + branchId + " not found"));

        // 2. Validate at least 2 unique services
        long uniqueServiceCount = request.getItems().stream()
                .map(BundleItemRequest::getServiceId)
                .distinct()
                .count();
        if (uniqueServiceCount < 2) {
            throw new BusinessException("Combo/gói dịch vụ phải có ít nhất 2 dịch vụ khác nhau");
        }

        // 3. Save the ServiceBundle first to generate its ID
        ServiceBundle bundle = ServiceBundle.builder()
                .branch(branch)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .isActive(true)
                .build();
        bundle = serviceBundleRepository.saveAndFlush(bundle);

        // 4. Create and add items
        List<ServiceBundleItem> items = new ArrayList<>();
        for (BundleItemRequest itemReq : request.getItems()) {
            Service service = serviceRepository.findById(itemReq.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Service with id " + itemReq.getServiceId() + " not found"));

            if (!service.getBranch().getId().equals(branchId)) {
                throw new BusinessException("Service with id " + service.getId()
                        + " does not belong to branch " + branchId);
            }
            if (!Boolean.TRUE.equals(service.getIsActive())) {
                throw new BusinessException("Service with id " + service.getId()
                        + " is inactive");
            }

            ServiceBundleItem item = ServiceBundleItem.builder()
                    .id(new ServiceBundleItemId(bundle.getId(), service.getId()))
                    .bundle(bundle)
                    .service(service)
                    .displayOrder(itemReq.getDisplayOrder())
                    .build();

            items.add(item);
        }
        bundle.getItems().addAll(items);
        serviceBundleRepository.saveAndFlush(bundle);

        // 5. Reload to fetch trigger-computed values
        ServiceBundle reloadedBundle = findOwnedBundle(branchId, bundle.getId());
        return toResponse(reloadedBundle);
    }

    @Override
    public List<BundleResponse> getByBranch(Long branchId) {
        ensureBranchExists(branchId);
        return serviceBundleRepository.findByBranchId(branchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<BundleResponse> getByBranchActiveOnly(Long branchId) {
        ensureBranchExists(branchId);
        return serviceBundleRepository.findByBranchIdAndIsActiveTrue(branchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BundleResponse getById(Long branchId, Long bundleId) {
        ServiceBundle bundle = findOwnedBundle(branchId, bundleId);
        return toResponse(bundle);
    }

    @Override
    @Transactional
    public BundleResponse update(Long branchId, Long bundleId, UpdateBundleRequest request) {
        ServiceBundle bundle = findOwnedBundle(branchId, bundleId);

        // 1. Validate at least 2 unique services
        long uniqueServiceCount = request.getItems().stream()
                .map(BundleItemRequest::getServiceId)
                .distinct()
                .count();
        if (uniqueServiceCount < 2) {
            throw new BusinessException("Combo/gói dịch vụ phải có ít nhất 2 dịch vụ khác nhau");
        }

        // 2. Update basic fields
        bundle.setName(request.getName());
        bundle.setDescription(request.getDescription());
        bundle.setPrice(request.getPrice());
        if (request.getIsActive() != null) {
            bundle.setIsActive(request.getIsActive());
        }

        // 3. Clear old items and flush to invoke delete triggers
        bundle.getItems().clear();
        serviceBundleRepository.saveAndFlush(bundle);

        // 4. Attach new items
        List<ServiceBundleItem> newItems = new ArrayList<>();
        for (BundleItemRequest itemReq : request.getItems()) {
            Service service = serviceRepository.findById(itemReq.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Service with id " + itemReq.getServiceId() + " not found"));

            if (!service.getBranch().getId().equals(branchId)) {
                throw new BusinessException("Service with id " + service.getId()
                        + " does not belong to branch " + branchId);
            }
            if (!Boolean.TRUE.equals(service.getIsActive())) {
                throw new BusinessException("Service with id " + service.getId()
                        + " is inactive");
            }

            ServiceBundleItem item = ServiceBundleItem.builder()
                    .id(new ServiceBundleItemId(bundle.getId(), service.getId()))
                    .bundle(bundle)
                    .service(service)
                    .displayOrder(itemReq.getDisplayOrder())
                    .build();

            newItems.add(item);
        }
        bundle.getItems().addAll(newItems);
        serviceBundleRepository.saveAndFlush(bundle);

        // 5. Reload to fetch trigger-computed values
        ServiceBundle reloadedBundle = findOwnedBundle(branchId, bundle.getId());
        return toResponse(reloadedBundle);
    }

    @Override
    @Transactional
    public void delete(Long branchId, Long bundleId) {
        ServiceBundle bundle = findOwnedBundle(branchId, bundleId);
        serviceBundleRepository.delete(bundle);
    }

    // ── Helpers ────────────────────────────────────────────────

    private void ensureBranchExists(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException(
                    "Branch with id " + branchId + " not found");
        }
    }

    private ServiceBundle findOwnedBundle(Long branchId, Long bundleId) {
        return serviceBundleRepository.findByIdAndBranchId(bundleId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service bundle with id " + bundleId
                                + " not found in branch " + branchId));
    }

    private BundleResponse toResponse(ServiceBundle bundle) {
        List<BundleItemResponse> itemResponses = bundle.getItems().stream()
                .sorted((a, b) -> {
                    Integer oa = a.getDisplayOrder() == null ? 0 : a.getDisplayOrder();
                    Integer ob = b.getDisplayOrder() == null ? 0 : b.getDisplayOrder();
                    return oa.compareTo(ob);
                })
                .map(item -> BundleItemResponse.builder()
                        .serviceId(item.getService().getId())
                        .name(item.getService().getName())
                        .price(item.getService().getPrice())
                        .durationMinutes(item.getService().getDurationMinutes())
                        .displayOrder(item.getDisplayOrder())
                        .build())
                .toList();

        return BundleResponse.builder()
                .id(bundle.getId())
                .branchId(bundle.getBranch().getId())
                .name(bundle.getName())
                .description(bundle.getDescription())
                .price(bundle.getPrice())
                .originalPrice(bundle.getOriginalPrice() != null ? bundle.getOriginalPrice() : java.math.BigDecimal.ZERO)
                .totalDurationMinutes(bundle.getTotalDurationMinutes() != null ? bundle.getTotalDurationMinutes() : 0)
                .isActive(bundle.getIsActive())
                .items(itemResponses)
                .build();
    }
}
