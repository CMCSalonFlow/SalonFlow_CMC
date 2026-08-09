package com.example.salonflow.ai.controller;

import com.example.salonflow.ai.dto.description.ServiceDescriptionGenerateRequest;
import com.example.salonflow.ai.dto.description.ServiceDescriptionGenerateResponse;
import com.example.salonflow.ai.dto.description.ServiceDescriptionQuotaResponse;
import com.example.salonflow.ai.service.ServiceDescriptionGenerationService;
import com.example.salonflow.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/salons/{salonId}/ai/service-descriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
public class OwnerServiceDescriptionAiController {

    private final ServiceDescriptionGenerationService serviceDescriptionGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<ServiceDescriptionGenerateResponse> generate(
            @PathVariable Long salonId,
            @Valid @RequestBody ServiceDescriptionGenerateRequest request
    ) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        ServiceDescriptionGenerateRequest normalizedRequest = new ServiceDescriptionGenerateRequest(
                salonId,
                request.serviceName(),
                request.keywords()
        );
        return ResponseEntity.ok(
                serviceDescriptionGenerationService.generate(ownerId, normalizedRequest)
        );
    }

    @GetMapping("/quota")
    public ResponseEntity<ServiceDescriptionQuotaResponse> quota(
            @PathVariable Long salonId
    ) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                serviceDescriptionGenerationService.getQuota(ownerId, salonId)
        );
    }
}
