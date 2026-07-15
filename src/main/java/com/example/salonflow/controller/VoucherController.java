package com.example.salonflow.controller;

import com.example.salonflow.dto.voucher.*;
import com.example.salonflow.services.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    /**
     * POST /api/vouchers
     * Tạo 1 voucher đơn lẻ (Admin)
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<VoucherResponse> create(
            @Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voucherService.createVoucher(request));
    }

    /**
     * POST /api/vouchers/batch
     * Tạo batch voucher với prefix (Admin)
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<VoucherResponse>> createBatch(
            @Valid @RequestBody BatchCreateVoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voucherService.createBatch(request));
    }

    /**
     * GET /api/vouchers
     * Lấy toàn bộ voucher (Admin)
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<VoucherResponse>> getAll() {
        return ResponseEntity.ok(voucherService.getAll());
    }

    /**
     * PATCH /api/vouchers/{id}/deactivate
     * Vô hiệu hóa voucher (Admin)
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<VoucherResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.deactivate(id));
    }

    // ─── Customer endpoints ───────────────────────────────────────────────────

    /**
     * POST /api/vouchers/validate
     * Kiểm tra voucher hợp lệ cho user hiện tại.
     * Truyền thêm orderTotal (optional) để tính discountAmount ngay tại checkout.
     */
    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ValidateVoucherResponse> validate(
            @Valid @RequestBody ValidateVoucherRequest request,
            @RequestParam(required = false) BigDecimal orderTotal) {
        return ResponseEntity.ok(voucherService.validate(request.getCode(), orderTotal));
    }
}
