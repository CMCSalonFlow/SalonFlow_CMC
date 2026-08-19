package com.example.salonflow.controller;

import com.example.salonflow.dto.voucher.*;
import com.example.salonflow.services.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<List<VoucherResponse>> getAllVouchers() {
        return ResponseEntity.ok(voucherService.getAllVouchers());
    }

    @PostMapping
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.ok(voucherService.createVoucher(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<VoucherResponse>> createBatchVouchers(@Valid @RequestBody CreateVoucherBatchRequest request) {
        return ResponseEntity.ok(voucherService.createBatchVouchers(request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateVoucher(@PathVariable Long id) {
        voucherService.deactivateVoucher(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateVoucherResponse> validateVoucher(
            @Valid @RequestBody ValidateVoucherRequest request,
            @RequestParam(required = false) BigDecimal orderTotal
    ) {
        return ResponseEntity.ok(voucherService.validateVoucher(request.getCode(), orderTotal));
    }
}
