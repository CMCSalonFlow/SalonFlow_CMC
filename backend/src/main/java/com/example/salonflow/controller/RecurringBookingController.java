package com.example.salonflow.controller;

import com.example.salonflow.dto.recurring.*;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.RecurringBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RecurringBookingController
 *
 * POST /api/v1/recurring-bookings/preview  → Tính trước, KHÔNG lưu DB
 * POST /api/v1/recurring-bookings/confirm  → Tạo thật các booking
 * DELETE /api/v1/recurring-bookings/{id}   → Hủy toàn bộ chuỗi
 */
@RestController
@RequestMapping("/api/v1/recurring-bookings")
@RequiredArgsConstructor
public class RecurringBookingController {

    private final RecurringBookingService recurringBookingService;

    /**
     * Xem trước danh sách ngày sẽ được đặt theo pattern lặp,
     * kèm đánh dấu ngày nào bị conflict.
     *
     * Request:
     * {
     *   "branchId": 1, "staffId": 5, "serviceId": 3,
     *   "pattern": "WEEKLY",
     *   "startDate": "2026-07-01", "endDate": "2026-09-01",
     *   "startTime": "09:00", "endTime": "10:00"
     * }
     */
    @PostMapping("/preview")
    public ResponseEntity<RecurringBookingPreviewResponse> preview(
            @Valid @RequestBody RecurringBookingRequest request
    ) {
        Long customerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                recurringBookingService.preview(customerId, request));
    }

    /**
     * Tạo thật các booking sau khi user đã xem preview và
     * quyết định xử lý từng ngày bị conflict.
     */
    @PostMapping("/confirm")
    public ResponseEntity<RecurringBookingResponse> confirm(
            @Valid @RequestBody RecurringBookingConfirmRequest request
    ) {
        Long customerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                recurringBookingService.confirm(customerId, request));
    }

    /**
     * Hủy toàn bộ chuỗi recurring booking (các booking chưa diễn ra).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id
    ) {
        Long customerId = SecurityUtils.getCurrentUserId();
        recurringBookingService.cancelRecurring(customerId, id);
        return ResponseEntity.noContent().build();
    }
}
