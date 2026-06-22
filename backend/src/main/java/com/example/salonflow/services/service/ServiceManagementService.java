package com.example.salonflow.services.service;

import com.example.salonflow.dto.service.CreateServiceRequest;
import com.example.salonflow.dto.service.ServiceResponse;
import com.example.salonflow.dto.service.UpdateServiceRequest;

import java.util.List;

/**
 * Đặt tên ServiceManagementService (thay vì ServiceService) để tránh
 * trùng / gây nhầm lẫn với annotation @Service của Spring và với
 * entity com.example.salonflow.entity.Service.
 */
public interface ServiceManagementService {

    ServiceResponse create(
            Long salonId,
            CreateServiceRequest request
    );

    List<ServiceResponse> getBySalon(Long salonId);

    ServiceResponse getById(Long salonId, Long serviceId);

    ServiceResponse update(
            Long salonId,
            Long serviceId,
            UpdateServiceRequest request
    );

    void delete(Long salonId, Long serviceId);
}