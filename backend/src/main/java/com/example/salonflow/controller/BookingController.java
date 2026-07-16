package com.example.salonflow.controller;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.booking.CreateGuestBookingRequest;
import com.example.salonflow.dto.booking.CreateBookingRequest;
import com.example.salonflow.dto.booking.BookingResponse;
import com.example.salonflow.dto.booking.LockSlotRequest;
import com.example.salonflow.dto.booking.LockSlotResponse;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.BookingService;
import com.example.salonflow.services.service.SlotLockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.salonflow.dto.booking.CreateWalkInBookingRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller quản lý các API đặt lịch (Booking) và giữ chỗ (Slot Lock).
 */
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final SlotLockService slotLockService;

    // API POST: Khởi tạo một đơn đặt lịch hẹn mới
    @PostMapping("/api/v1/branches/{branchId}/bookings")
    public ResponseEntity<BookingResponse> create(
            @PathVariable Long branchId,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.create(branchId, request));
    }

    // API POST public: Khách vãng lai tạo booking không cần đăng nhập
    @PostMapping("/api/v1/branches/{branchId}/guest-bookings")
    public ResponseEntity<BookingResponse> createGuestBooking(
            @PathVariable Long branchId,
            @Valid @RequestBody CreateGuestBookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.createGuestBooking(branchId, request));
    }

    // API GET: Lấy toàn bộ danh sách lịch hẹn của chi nhánh
    @GetMapping("/api/v1/branches/{branchId}/bookings")
    public ResponseEntity<List<BookingResponse>> getByBranch(
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(bookingService.getByBranch(branchId));
    }

    // API GET: Lấy chi tiết thông tin của một lịch hẹn cụ thể
    @GetMapping("/api/v1/branches/{branchId}/bookings/{bookingId}")
    public ResponseEntity<BookingResponse> getById(
            @PathVariable Long branchId,
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(bookingService.getById(branchId, bookingId));
    }

    // API GET: Truy vấn các khung giờ trống khả dụng thời gian thực
    @GetMapping("/api/v1/branches/{branchId}/bookings/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) List<Long> serviceIds,
            @RequestParam(required = false) Long bundleId,
            @RequestParam(required = false) Long staffId
    ) {
        return ResponseEntity.ok(bookingService.getAvailability(branchId, date, serviceIds, bundleId, staffId));
    }

   @PostMapping("/api/v1/branches/{branchId}/walk-in-bookings")
   public ResponseEntity<BookingResponse> createWalkInBooking(
           @PathVariable Long branchId,
           @Valid @RequestBody CreateWalkInBookingRequest request
    ) {
        return ResponseEntity.ok( bookingService.createWalkInBooking(branchId, request));
   }

    /**
     * Lock slot khi user chọn khung giờ.
     */
    @PostMapping("/api/v1/bookings/lock")
    public ResponseEntity<LockSlotResponse> lockSlot(
            @Valid @RequestBody LockSlotRequest request
    ) {
        Long customerId = SecurityUtils.getCurrentUserId();
        LockSlotResponse response = slotLockService.lockSlot(customerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Unlock slot khi user hủy đặt lịch hoặc thoát trang.
     */
    @DeleteMapping("/api/v1/bookings/lock")
    public ResponseEntity<Void> unlockSlot(
            @RequestParam String slotKey
    ) {
        Long customerId = SecurityUtils.getCurrentUserId();
        slotLockService.unlockSlot(customerId, slotKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * Kiểm tra slot có đang bị lock không.
     */
    @GetMapping("/api/v1/bookings/lock")
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
