package com.example.salonflow.services.service;

import com.example.salonflow.dto.staff.CreateStaffRequest;
import com.example.salonflow.dto.staff.StaffResponse;
import com.example.salonflow.dto.staff.UpdateStaffRequest;

import java.util.List;

/**
 * Giao diện định nghĩa các nghiệp vụ quản lý nhân viên (Staff).
 */
public interface StaffService {

    // Thêm mới nhân viên vào chi nhánh (Branch)
    StaffResponse create(Long branchId, CreateStaffRequest request);

    // Lấy danh sách toàn bộ nhân viên thuộc một chi nhánh (Branch)
    List<StaffResponse> getByBranch(Long branchId);

    // Lấy chi tiết thông tin một nhân viên theo ID
    StaffResponse getById(Long branchId, Long staffId);

    // Cập nhật thông tin chi tiết của nhân viên
    StaffResponse update(Long branchId, Long staffId, UpdateStaffRequest request);

    // Xóa nhân viên khỏi chi nhánh (Branch)
    void delete(Long branchId, Long staffId);
}
