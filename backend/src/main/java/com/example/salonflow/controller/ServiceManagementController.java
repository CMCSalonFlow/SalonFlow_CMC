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
@RequestMapping("/api/v1/branches/{branchId}/services")
@RequiredArgsConstructor
public class ServiceManagementController {

    private final ServiceManagementService serviceManagementService;

    @PostMapping
    public ResponseEntity<ServiceResponse> create(
            @PathVariable Long branchId,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        return ResponseEntity.ok(
                serviceManagementService.create(branchId, request));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getByBranch(
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(
                serviceManagementService.getByBranch(branchId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<ServiceResponse>> getPublicByBranch(
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(
                serviceManagementService.getByBranchActiveOnly(branchId));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceResponse> getById(
            @PathVariable Long branchId,
            @PathVariable Long serviceId
    ) {
        return ResponseEntity.ok(
                serviceManagementService.getById(branchId, serviceId));
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceResponse> update(
            @PathVariable Long branchId,
            @PathVariable Long serviceId,
            @Valid @RequestBody UpdateServiceRequest request
    ) {
        return ResponseEntity.ok(
                serviceManagementService.update(branchId, serviceId, request));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long branchId,
            @PathVariable Long serviceId
    ) {
        serviceManagementService.delete(branchId, serviceId);
        return ResponseEntity.noContent().build();
    }
}
