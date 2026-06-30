package com.example.salonflow.controller;

import com.example.salonflow.dto.booking.LockSlotRequest;
import com.example.salonflow.dto.booking.LockSlotResponse;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.SlotLockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * BookingController
 *
 * POST /api/v1/bookings/lock    → Lock slot (Redis SETNX)
 * DELETE /api/v1/bookings/lock  → Unlock slot (hủy)
 * GET /api/v1/bookings/lock     → Kiểm tra slot có bị lock không
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final SlotLockService slotLockService;

    /**
     * Lock slot khi user chọn khung giờ.
     *
     * Request body:
     * {
     *   "branchId": 1,
     *   "staffId": 5,
     *   "serviceId": 3,
     *   "bookingDate": "2026-06-30",
     *   "startTime": "09:00"
     * }
     *
     * Response 200: { slotKey, ttlSeconds, message }
     * Response 409: Slot đã bị lock bởi người khác
     */
    @PostMapping("/lock")
    public ResponseEntity<LockSlotResponse> lockSlot(
            @Valid @RequestBody LockSlotRequest request
    ) {
        Long customerId = SecurityUtils.getCurrentUserId();
        LockSlotResponse response = slotLockService.lockSlot(customerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Unlock slot khi user hủy đặt lịch hoặc thoát trang.
     *
     * Request param: slotKey
     * VD: DELETE /api/v1/bookings/lock?slotKey=slot:1:5:2026-06-30:09:00
     */
    @DeleteMapping("/lock")
    public ResponseEntity<Void> unlockSlot(
            @RequestParam String slotKey
    ) {
        Long customerId = SecurityUtils.getCurrentUserId();
        slotLockService.unlockSlot(customerId, slotKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * Kiểm tra slot có đang bị lock không.
     * FE dùng để hiển thị slot màu xám.
     *
     * Response: { locked: true/false, ttlSeconds: 540 }
     */
    @GetMapping("/lock")
    public ResponseEntity<?> checkSlotLock(
            @RequestParam String slotKey
    ) {
        boolean locked = slotLockService.isSlotLocked(slotKey);
        Long ttl = slotLockService.getSlotTtl(slotKey);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "locked", locked,
                        "ttlSeconds", ttl
                )
        );
    }
}
