package com.example.salonflow.services.impl;

import com.example.salonflow.dto.service.CreateServiceRequest;
import com.example.salonflow.dto.service.ServiceResponse;
import com.example.salonflow.dto.service.UpdateServiceRequest;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.Service;
import com.example.salonflow.entity.ServiceCategory;
import com.example.salonflow.entity.ServiceImage;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.repository.ServiceCategoryRepository;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.services.service.ServiceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

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
public class ServiceManagementServiceImpl implements ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final SalonRepository salonRepository;
    private final ServiceCategoryRepository categoryRepository;

    @Override
    @Transactional
    public ServiceResponse create(
            Long salonId,
            CreateServiceRequest request
    ) {

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Salon with id " + salonId + " not found"));

        ServiceCategory category = resolveCategory(request.getCategoryId());

        Service service = Service.builder()
                .salon(salon)
                .category(category)
                .name(request.getName())
                .price(request.getPrice())
                .durationMinutes(request.getDurationMinutes())
                .description(request.getDescription())
                .isActive(true)
                .build();

        attachImages(service, request.getImages());

        service = serviceRepository.save(service);

        return toResponse(service);
    }

    @Override
    public List<ServiceResponse> getBySalon(Long salonId) {

        ensureSalonExists(salonId);

        return serviceRepository.findBySalonId(salonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ServiceResponse getById(Long salonId, Long serviceId) {

        Service service = findOwnedService(salonId, serviceId);

        return toResponse(service);
    }

    @Override
    @Transactional
    public ServiceResponse update(
            Long salonId,
            Long serviceId,
            UpdateServiceRequest request
    ) {

        Service service = findOwnedService(salonId, serviceId);

        ServiceCategory category = resolveCategory(request.getCategoryId());

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

        service = serviceRepository.save(service);

        return toResponse(service);
    }

    @Override
    @Transactional
    public void delete(Long salonId, Long serviceId) {

        Service service = findOwnedService(salonId, serviceId);

        serviceRepository.delete(service);
    }

    // ── Helpers ────────────────────────────────────────────────

    private void ensureSalonExists(Long salonId) {
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException(
                    "Salon with id " + salonId + " not found");
        }
    }

    private Service findOwnedService(Long salonId, Long serviceId) {
        return serviceRepository.findByIdAndSalonId(serviceId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service with id " + serviceId
                                + " not found in salon " + salonId));
    }

    private ServiceCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with id " + categoryId + " not found"));
    }

    private void attachImages(Service service, List<String> imageUrls) {
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

    private ServiceResponse toResponse(Service service) {

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
                .salonId(service.getSalon().getId())
                .categoryId(service.getCategory() != null
                        ? service.getCategory().getId() : null)
                .categoryName(service.getCategory() != null
                        ? service.getCategory().getName() : null)
                .name(service.getName())
                .price(service.getPrice())
                .durationMinutes(service.getDurationMinutes())
                .description(service.getDescription())
                .isActive(service.getIsActive())
                .images(imageUrls)
                .build();
    }
}