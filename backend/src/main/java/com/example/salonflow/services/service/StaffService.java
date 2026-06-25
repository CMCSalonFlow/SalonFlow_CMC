package com.example.salonflow.services.service;

import com.example.salonflow.dto.staff.CreateStaffRequest;
import com.example.salonflow.dto.staff.StaffResponse;
import com.example.salonflow.dto.staff.UpdateStaffRequest;

import java.util.List;

/**
 * Giao diện định nghĩa các nghiệp vụ quản lý nhân viên (Staff).
 */
public interface StaffService {

    // Thêm mới nhân viên vào salon
    StaffResponse create(Long salonId, CreateStaffRequest request);

    // Lấy danh sách toàn bộ nhân viên thuộc một salon
    List<StaffResponse> getBySalon(Long salonId);

    // Lấy chi tiết thông tin một nhân viên theo ID
    StaffResponse getById(Long salonId, Long staffId);

    // Cập nhật thông tin chi tiết của nhân viên
    StaffResponse update(Long salonId, Long staffId, UpdateStaffRequest request);

    // Xóa nhân viên khỏi salon
    void delete(Long salonId, Long staffId);
}
