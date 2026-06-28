package com.example.salonflow.controller;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.booking.CreateBookingRequest;
import com.example.salonflow.dto.booking.BookingResponse;
import com.example.salonflow.services.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller quản lý các API đặt lịch (Booking) tại chi nhánh.
 * Đường dẫn gốc: /api/v1/branches/{branchId}/bookings
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // API POST: Khởi tạo một đơn đặt lịch hẹn mới
    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @PathVariable Long branchId,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.create(branchId, request));
    }

    // API GET: Lấy toàn bộ danh sách lịch hẹn của chi nhánh
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getByBranch(
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(bookingService.getByBranch(branchId));
    }

    // API GET: Lấy chi tiết thông tin của một lịch hẹn cụ thể
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getById(
            @PathVariable Long branchId,
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(bookingService.getById(branchId, bookingId));
    }

    // API GET: Truy vấn các khung giờ trống khả dụng thời gian thực (Real-time Availability check)
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) List<Long> serviceIds,
            @RequestParam(required = false) Long bundleId,
            @RequestParam(required = false) Long staffId
    ) {
        return ResponseEntity.ok(bookingService.getAvailability(branchId, date, serviceIds, bundleId, staffId));
    }
}
