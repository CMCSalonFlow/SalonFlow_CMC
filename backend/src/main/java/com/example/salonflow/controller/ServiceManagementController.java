package com.example.salonflow.controller;

import com.example.salonflow.dto.service.CreateServiceRequest;
import com.example.salonflow.dto.service.ServiceResponse;
import com.example.salonflow.dto.service.UpdateServiceRequest;
import com.example.salonflow.services.service.ServiceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD dịch vụ cụ thể của 1 salon.
 * Route: /api/v1/salons/{salonId}/services
 */
@RestController
@RequestMapping("/api/v1/salons/{salonId}/services")
@RequiredArgsConstructor
public class ServiceManagementController {

    private final ServiceManagementService serviceManagementService;

    @PostMapping
    public ResponseEntity<ServiceResponse> create(
            @PathVariable Long salonId,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        return ResponseEntity.ok(
                serviceManagementService.create(salonId, request));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getBySalon(
            @PathVariable Long salonId
    ) {
        return ResponseEntity.ok(
                serviceManagementService.getBySalon(salonId));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceResponse> getById(
            @PathVariable Long salonId,
            @PathVariable Long serviceId
    ) {
        return ResponseEntity.ok(
                serviceManagementService.getById(salonId, serviceId));
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceResponse> update(
            @PathVariable Long salonId,
            @PathVariable Long serviceId,
            @Valid @RequestBody UpdateServiceRequest request
    ) {
        return ResponseEntity.ok(
                serviceManagementService.update(salonId, serviceId, request));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long salonId,
            @PathVariable Long serviceId
    ) {
        serviceManagementService.delete(salonId, serviceId);
        return ResponseEntity.noContent().build();
    }
}