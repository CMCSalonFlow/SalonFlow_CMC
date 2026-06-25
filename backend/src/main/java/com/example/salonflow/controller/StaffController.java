package com.example.salonflow.controller;

import com.example.salonflow.dto.staff.CreateStaffRequest;
import com.example.salonflow.dto.staff.StaffResponse;
import com.example.salonflow.dto.staff.UpdateStaffRequest;
import com.example.salonflow.services.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các API quản lý nhân viên của Salon.
 * Đường dẫn gốc: /api/v1/salons/{salonId}/staff
 */
@RestController
@RequestMapping("/api/v1/salons/{salonId}/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    // API POST: Thêm mới nhân viên vào salon
    @PostMapping
    public ResponseEntity<StaffResponse> create(
            @PathVariable Long salonId,
            @Valid @RequestBody CreateStaffRequest request
    ) {
        return ResponseEntity.ok(staffService.create(salonId, request));
    }

    // API GET: Lấy danh sách nhân viên của salon
    @GetMapping
    public ResponseEntity<List<StaffResponse>> getBySalon(
            @PathVariable Long salonId
    ) {
        return ResponseEntity.ok(staffService.getBySalon(salonId));
    }

    // API GET: Lấy thông tin chi tiết một nhân viên cụ thể
    @GetMapping("/{staffId}")
    public ResponseEntity<StaffResponse> getById(
            @PathVariable Long salonId,
            @PathVariable Long staffId
    ) {
        return ResponseEntity.ok(staffService.getById(salonId, staffId));
    }

    // API PUT: Cập nhật thông tin nhân viên
    @PutMapping("/{staffId}")
    public ResponseEntity<StaffResponse> update(
            @PathVariable Long salonId,
            @PathVariable Long staffId,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        return ResponseEntity.ok(staffService.update(salonId, staffId, request));
    }

    // API DELETE: Xóa nhân viên khỏi salon
    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long salonId,
            @PathVariable Long staffId
    ) {
        staffService.delete(salonId, staffId);
        return ResponseEntity.noContent().build();
    }
}
