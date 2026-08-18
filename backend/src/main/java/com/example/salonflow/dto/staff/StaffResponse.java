package com.example.salonflow.dto.staff;

import com.example.salonflow.dto.service.ServiceResponse;
import lombok.*;

import java.util.List;

/**
 * Phản hồi thông tin nhân viên trả về cho phía FE (StaffResponse).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResponse {

    // ID của nhân viên
    private Long id;

    // ID của Chi nhánh (Branch) chứa nhân viên này
    private Long branchId;

    // Tên nhân viên
    private String name;

    // Ảnh đại diện
    private String avatarUrl;

    // Tiểu sử
    private String bio;

    // Chuyên môn/tag kỹ năng
    private String specialties;

    // Danh sách các dịch vụ nhân viên này được phép thực hiện
    private List<ServiceResponse> services;

    // ID của tài khoản người dùng liên kết
    private Long userId;

    private String email;

    private String phone;

    private String roleCode;
}

