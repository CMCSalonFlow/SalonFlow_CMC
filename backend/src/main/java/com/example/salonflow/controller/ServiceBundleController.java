package com.example.salonflow.controller;

import com.example.salonflow.dto.bundle.CreateBundleRequest;
import com.example.salonflow.dto.bundle.BundleResponse;
import com.example.salonflow.dto.bundle.UpdateBundleRequest;
import com.example.salonflow.services.service.ServiceBundleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/bundles")
@RequiredArgsConstructor
public class ServiceBundleController {

    private final ServiceBundleService serviceBundleService;

    @PostMapping
    public ResponseEntity<BundleResponse> create(
            @PathVariable Long branchId,
            @Valid @RequestBody CreateBundleRequest request
    ) {
        return ResponseEntity.ok(
                serviceBundleService.create(branchId, request));
    }

    @GetMapping
    public ResponseEntity<List<BundleResponse>> getByBranch(
            @PathVariable Long branchId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        if (activeOnly) {
            return ResponseEntity.ok(
                    serviceBundleService.getByBranchActiveOnly(branchId));
        }
        return ResponseEntity.ok(
                serviceBundleService.getByBranch(branchId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<BundleResponse>> getPublicByBranch(
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(
                serviceBundleService.getByBranchActiveOnly(branchId));
    }

    @GetMapping("/{bundleId}")
    public ResponseEntity<BundleResponse> getById(
            @PathVariable Long branchId,
            @PathVariable Long bundleId
    ) {
        return ResponseEntity.ok(
                serviceBundleService.getById(branchId, bundleId));
    }

    @PutMapping("/{bundleId}")
    public ResponseEntity<BundleResponse> update(
            @PathVariable Long branchId,
            @PathVariable Long bundleId,
            @Valid @RequestBody UpdateBundleRequest request
    ) {
        return ResponseEntity.ok(
                serviceBundleService.update(branchId, bundleId, request));
    }

    @DeleteMapping("/{bundleId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long branchId,
            @PathVariable Long bundleId
    ) {
        serviceBundleService.delete(branchId, bundleId);
        return ResponseEntity.noContent().build();
    }
}
