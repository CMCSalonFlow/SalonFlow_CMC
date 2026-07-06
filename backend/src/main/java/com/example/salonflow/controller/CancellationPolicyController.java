package com.example.salonflow.controller;

import com.example.salonflow.dto.cancellation.CancellationPolicyResponse;
import com.example.salonflow.dto.cancellation.UpdateCancellationPolicyRequest;
import com.example.salonflow.services.service.CancellationPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/salons")
@RequiredArgsConstructor
public class CancellationPolicyController {

    private final CancellationPolicyService cancellationPolicyService;

    // Lấy chính sách hủy của salon
    @GetMapping("/{salonId}/cancellation-policy")
    public ResponseEntity<CancellationPolicyResponse> getPolicy(@PathVariable Long salonId) {
        CancellationPolicyResponse response = cancellationPolicyService.getBySalonId(salonId);
        return ResponseEntity.ok(response);
    }

    // Cập nhật chính sách hủy (chỉ Owner)
    @PreAuthorize("hasRole('SALON_OWNER')")
    @PutMapping("/{salonId}/cancellation-policy")
    public ResponseEntity<CancellationPolicyResponse> updatePolicy(
            @PathVariable Long salonId,
            @RequestBody @Valid UpdateCancellationPolicyRequest request) {
        
        CancellationPolicyResponse response = cancellationPolicyService.updatePolicy(salonId, request);
        return ResponseEntity.ok(response);
    }
}