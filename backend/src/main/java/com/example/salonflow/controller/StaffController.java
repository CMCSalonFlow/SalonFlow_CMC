package com.example.salonflow.controller;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.staff.CreateStaffRequest;
import com.example.salonflow.dto.staff.PublicStaffResponse;
import com.example.salonflow.dto.staff.StaffResponse;
import com.example.salonflow.dto.staff.UpdateStaffRequest;
import com.example.salonflow.services.service.BookingService;
import com.example.salonflow.services.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller xử lý các API quản lý nhân viên của Chi nhánh (Branch).
 * Đường dẫn gốc: /api/v1/branches/{branchId}/staff
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final BookingService bookingService;

    // API POST: Thêm mới nhân viên vào chi nhánh
    @PostMapping
    public ResponseEntity<StaffResponse> create(
            @PathVariable Long branchId,
            @Valid @RequestBody CreateStaffRequest request
    ) {
        return ResponseEntity.ok(staffService.create(branchId, request));
    }

    // API GET: Lấy danh sách nhân viên của chi nhánh
    @GetMapping
    public ResponseEntity<List<StaffResponse>> getByBranch(
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(staffService.getByBranch(branchId));
    }

    // API GET public: Lấy danh sách nhân viên public của chi nhánh cho bước chọn staff
    @GetMapping("/public")
    public ResponseEntity<List<PublicStaffResponse>> getPublicByBranch(
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(staffService.getPublicByBranch(branchId));
    }

    // API GET public: Lấy khung giờ available của 1 nhân viên theo ngày cho bước chọn giờ
    @GetMapping("/{staffId}/availability")
    public ResponseEntity<AvailabilityResponse> getPublicAvailability(
            @PathVariable Long branchId,
            @PathVariable Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) List<Long> serviceIds,
            @RequestParam(required = false) Long bundleId
    ) {
        return ResponseEntity.ok(
                bookingService.getAvailability(branchId, date, serviceIds, bundleId, staffId)
        );
    }

    // API GET: Lấy thông tin chi tiết một nhân viên cụ thể
    @GetMapping("/{staffId}")
    public ResponseEntity<StaffResponse> getById(
            @PathVariable Long branchId,
            @PathVariable Long staffId
    ) {
        return ResponseEntity.ok(staffService.getById(branchId, staffId));
    }

    // API PUT: Cập nhật thông tin nhân viên
    @PutMapping("/{staffId}")
    public ResponseEntity<StaffResponse> update(
            @PathVariable Long branchId,
            @PathVariable Long staffId,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        return ResponseEntity.ok(staffService.update(branchId, staffId, request));
    }

    // API DELETE: Xóa nhân viên khỏi chi nhánh
    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long branchId,
            @PathVariable Long staffId
    ) {
        staffService.delete(branchId, staffId);
        return ResponseEntity.noContent().build();
    }
}
