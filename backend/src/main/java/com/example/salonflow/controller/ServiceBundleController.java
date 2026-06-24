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
@RequestMapping("/api/v1/salons/{salonId}/bundles")
@RequiredArgsConstructor
public class ServiceBundleController {

    private final ServiceBundleService serviceBundleService;

    @PostMapping
    public ResponseEntity<BundleResponse> create(
            @PathVariable Long salonId,
            @Valid @RequestBody CreateBundleRequest request
    ) {
        return ResponseEntity.ok(
                serviceBundleService.create(salonId, request));
    }

    @GetMapping
    public ResponseEntity<List<BundleResponse>> getBySalon(
            @PathVariable Long salonId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        if (activeOnly) {
            return ResponseEntity.ok(
                    serviceBundleService.getBySalonActiveOnly(salonId));
        }
        return ResponseEntity.ok(
                serviceBundleService.getBySalon(salonId));
    }

    @GetMapping("/{bundleId}")
    public ResponseEntity<BundleResponse> getById(
            @PathVariable Long salonId,
            @PathVariable Long bundleId
    ) {
        return ResponseEntity.ok(
                serviceBundleService.getById(salonId, bundleId));
    }

    @PutMapping("/{bundleId}")
    public ResponseEntity<BundleResponse> update(
            @PathVariable Long salonId,
            @PathVariable Long bundleId,
            @Valid @RequestBody UpdateBundleRequest request
    ) {
        return ResponseEntity.ok(
                serviceBundleService.update(salonId, bundleId, request));
    }

    @DeleteMapping("/{bundleId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long salonId,
            @PathVariable Long bundleId
    ) {
        serviceBundleService.delete(salonId, bundleId);
        return ResponseEntity.noContent().build();
    }
}
