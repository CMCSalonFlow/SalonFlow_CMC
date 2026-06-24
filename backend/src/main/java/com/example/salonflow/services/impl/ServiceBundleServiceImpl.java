package com.example.salonflow.services.impl;

import com.example.salonflow.dto.bundle.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.SalonRepository;
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
    private final SalonRepository salonRepository;

    @Override
    @Transactional
    public BundleResponse create(Long salonId, CreateBundleRequest request) {
        // 1. Ensure Salon exists
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Salon with id " + salonId + " not found"));

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
                .salon(salon)
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

            if (!service.getSalon().getId().equals(salonId)) {
                throw new BusinessException("Service with id " + service.getId()
                        + " does not belong to salon " + salonId);
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
        ServiceBundle reloadedBundle = findOwnedBundle(salonId, bundle.getId());
        return toResponse(reloadedBundle);
    }

    @Override
    public List<BundleResponse> getBySalon(Long salonId) {
        ensureSalonExists(salonId);
        return serviceBundleRepository.findBySalonId(salonId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<BundleResponse> getBySalonActiveOnly(Long salonId) {
        ensureSalonExists(salonId);
        return serviceBundleRepository.findBySalonIdAndIsActiveTrue(salonId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BundleResponse getById(Long salonId, Long bundleId) {
        ServiceBundle bundle = findOwnedBundle(salonId, bundleId);
        return toResponse(bundle);
    }

    @Override
    @Transactional
    public BundleResponse update(Long salonId, Long bundleId, UpdateBundleRequest request) {
        ServiceBundle bundle = findOwnedBundle(salonId, bundleId);

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

            if (!service.getSalon().getId().equals(salonId)) {
                throw new BusinessException("Service with id " + service.getId()
                        + " does not belong to salon " + salonId);
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
        ServiceBundle reloadedBundle = findOwnedBundle(salonId, bundle.getId());
        return toResponse(reloadedBundle);
    }

    @Override
    @Transactional
    public void delete(Long salonId, Long bundleId) {
        ServiceBundle bundle = findOwnedBundle(salonId, bundleId);
        serviceBundleRepository.delete(bundle);
    }

    // ── Helpers ────────────────────────────────────────────────

    private void ensureSalonExists(Long salonId) {
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException(
                    "Salon with id " + salonId + " not found");
        }
    }

    private ServiceBundle findOwnedBundle(Long salonId, Long bundleId) {
        return serviceBundleRepository.findByIdAndSalonId(bundleId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service bundle with id " + bundleId
                                + " not found in salon " + salonId));
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
                .salonId(bundle.getSalon().getId())
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
