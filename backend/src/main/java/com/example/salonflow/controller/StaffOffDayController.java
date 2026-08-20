package com.example.salonflow.controller;

import com.example.salonflow.dto.offday.*;
import com.example.salonflow.entity.LeaveStatus;
import com.example.salonflow.services.service.StaffOffDayService;
import com.example.salonflow.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StaffOffDayController {

    private final StaffOffDayService staffOffDayService;

    // ----------------------------------------------------
    // APIs QUẢN LÝ ĐƠN XIN NGHĨ PHÉP CÁ NHÂN (NEW)
    // ----------------------------------------------------

    // 1. Nhân viên / Quản lý tạo đơn xin nghỉ phép
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'BRANCH_MANAGER', 'SALON_OWNER')")
    @PostMapping("/api/v1/staff-leaves")
    public ResponseEntity<StaffLeaveResponse> createLeaveRequest(
            @RequestBody @Valid CreateLeaveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        StaffLeaveResponse response = staffOffDayService.createLeaveRequest(userId, request);
        return ResponseEntity.ok(response);
    }

    // 2. Nhân viên xem lịch sử đơn xin nghỉ của mình
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'BRANCH_MANAGER', 'SALON_OWNER')")
    @GetMapping("/api/v1/staff-leaves/my-requests")
    public ResponseEntity<List<StaffLeaveResponse>> getMyLeaveRequests() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<StaffLeaveResponse> list = staffOffDayService.getMyLeaveRequests(userId);
        return ResponseEntity.ok(list);
    }

    // 3. Nhân viên hủy đơn xin nghỉ phép (khi đang PENDING)
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'BRANCH_MANAGER', 'SALON_OWNER')")
    @DeleteMapping("/api/v1/staff-leaves/{id}/cancel")
    public ResponseEntity<Void> cancelLeaveRequest(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        staffOffDayService.cancelLeaveRequest(userId, id);
        return ResponseEntity.noContent().build();
    }

    // 4. Owner / Manager xem danh sách đơn xin nghỉ phép cần duyệt
    @PreAuthorize("hasAnyRole('SALON_OWNER', 'MANAGER', 'BRANCH_MANAGER')")
    @GetMapping("/api/v1/staff-leaves/approval-list")
    public ResponseEntity<List<StaffLeaveResponse>> getApprovalList(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Long branchId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<StaffLeaveResponse> list = staffOffDayService.getApprovalList(userId, status, branchId);
        return ResponseEntity.ok(list);
    }

    // Endpoint hiển thị công khai/cho toàn bộ nhân viên xem danh sách các đơn đã được duyệt để cập nhật lịch
    @GetMapping("/api/v1/staff-leaves/approved")
    public ResponseEntity<List<StaffLeaveResponse>> getApprovedLeaves(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<StaffLeaveResponse> list = staffOffDayService.getApprovedLeaves(branchId, startDate, endDate);
        return ResponseEntity.ok(list);
    }

    // 5. Owner / Manager phê duyệt đơn
    @PreAuthorize("hasAnyRole('SALON_OWNER', 'MANAGER', 'BRANCH_MANAGER')")
    @PatchMapping("/api/v1/staff-leaves/{id}/approve")
    public ResponseEntity<StaffLeaveResponse> approveLeaveRequest(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        StaffLeaveResponse response = staffOffDayService.approveLeaveRequest(userId, id);
        return ResponseEntity.ok(response);
    }

    // 6. Owner / Manager từ chối đơn
    @PreAuthorize("hasAnyRole('SALON_OWNER', 'MANAGER', 'BRANCH_MANAGER')")
    @PatchMapping("/api/v1/staff-leaves/{id}/reject")
    public ResponseEntity<StaffLeaveResponse> rejectLeaveRequest(
            @PathVariable Long id,
            @RequestBody @Valid RejectLeaveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        StaffLeaveResponse response = staffOffDayService.rejectLeaveRequest(userId, id, request);
        return ResponseEntity.ok(response);
    }

    // ----------------------------------------------------
    // LEGACY APIs
    // ----------------------------------------------------

    @PreAuthorize("hasRole('SALON_OWNER')")
    @PatchMapping("/api/v1/staff/{staffId}/off-days")
    public ResponseEntity<StaffOffDayResponse> createOffDay(
            @PathVariable Long staffId,
            @RequestBody @Valid StaffOffDayRequest request) {
        StaffOffDayResponse response = staffOffDayService.createOffDay(staffId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SALON_OWNER')")
    @GetMapping("/api/v1/staff/{staffId}/off-days")
    public ResponseEntity<List<StaffOffDayResponse>> getOffDays(
            @PathVariable Long staffId) {
        List<StaffOffDayResponse> offDays = staffOffDayService.getOffDaysByStaffId(staffId);
        return ResponseEntity.ok(offDays);
    }

    @PreAuthorize("hasRole('SALON_OWNER')")
    @PutMapping("/api/v1/staff/off-days/{offDayId}")
    public ResponseEntity<StaffOffDayResponse> updateOffDay(
            @PathVariable Long offDayId,
            @RequestBody @Valid StaffOffDayRequest request) {
        StaffOffDayResponse response = staffOffDayService.updateOffDay(offDayId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SALON_OWNER')")
    @DeleteMapping("/api/v1/staff/off-days/{offDayId}")
    public ResponseEntity<Void> deleteOffDay(@PathVariable Long offDayId) {
        staffOffDayService.deleteOffDay(offDayId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/staff/{staffId}/off-check")
    public ResponseEntity<Boolean> checkOffDay(
            @PathVariable Long staffId,
            @RequestParam LocalDate date) {
        boolean isOff = staffOffDayService.isStaffOffInPeriod(staffId, date);
        return ResponseEntity.ok(isOff);
    }
}