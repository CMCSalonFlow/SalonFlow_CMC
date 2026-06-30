package com.example.salonflow.controller;

import com.example.salonflow.dto.shift.*;
import com.example.salonflow.services.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    // ── Template CRUD ──────────────────────────────────────────

    @PostMapping("/templates")
    public ResponseEntity<ShiftTemplateResponse> createTemplate(
            @Valid @RequestBody CreateShiftTemplateRequest request
    ) {
        return ResponseEntity.ok(shiftService.createTemplate(request));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<ShiftTemplateResponse>> getTemplates(
            @RequestParam Long userId,
            @RequestParam Long branchId
    ) {
        return ResponseEntity.ok(
                shiftService.getTemplatesByUser(userId, branchId));
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<ShiftTemplateResponse> getTemplate(
            @PathVariable Long templateId
    ) {
        return ResponseEntity.ok(shiftService.getTemplateById(templateId));
    }

    @PutMapping("/templates/{templateId}")
    public ResponseEntity<ShiftTemplateResponse> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CreateShiftTemplateRequest request
    ) {
        return ResponseEntity.ok(
                shiftService.updateTemplate(templateId, request));
    }

    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long templateId
    ) {
        shiftService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    // ── Áp dụng template vào tuần ──────────────────────────────

    /**
     * POST /api/v1/shifts/templates/{templateId}/apply
     * Body: { "weekStartDate": "2026-06-23", "overwrite": false }
     *
     * Dùng cho cả 2 nút:
     *   - "Áp dụng tuần này"  → weekStartDate = thứ 2 tuần này
     *   - "Áp dụng tuần sau"  → weekStartDate = thứ 2 tuần sau
     */
    @PostMapping("/templates/{templateId}/apply")
    public ResponseEntity<List<ShiftResponse>> applyTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody ApplyTemplateRequest request
    ) {
        return ResponseEntity.ok(
                shiftService.applyTemplate(templateId, request));
    }

    // ── Query shifts ────────────────────────────────────────────

    @GetMapping("/user/{userId}/week")
    public ResponseEntity<List<ShiftResponse>> getShiftsByUserAndWeek(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStartDate
    ) {
        return ResponseEntity.ok(
                shiftService.getShiftsByUserAndWeek(userId, weekStartDate));
    }

    @GetMapping("/branch/{branchId}/date")
    public ResponseEntity<List<ShiftResponse>> getShiftsByBranchAndDate(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ResponseEntity.ok(
                shiftService.getShiftsByBranchAndDate(branchId, date));
    }

    // ── Availability slots cho booking ──────────────────────────

    /**
     * GET /api/v1/shifts/branch/{branchId}/availability?date=2026-06-23
     * Trả về danh sách slot làm việc của tất cả staff trong branch
     * vào ngày đó — dùng để booking calendar hiển thị khung giờ khả dụng.
     */
    @GetMapping("/branch/{branchId}/availability")
    public ResponseEntity<List<AvailabilitySlotResponse>> getAvailabilitySlots(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ResponseEntity.ok(
                shiftService.getAvailabilitySlots(branchId, date));
    }
}
