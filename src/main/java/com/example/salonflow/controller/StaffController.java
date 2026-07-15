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
 * Controller xử lý các API quản lý nhân viên của Chi nhánh (Branch).
 * Đường dẫn gốc: /api/v1/branches/{branchId}/staff
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

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
