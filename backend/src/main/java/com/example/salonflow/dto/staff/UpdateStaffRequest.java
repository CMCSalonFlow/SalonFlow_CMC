package com.example.salonflow.dto.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

/**
 * Yêu cầu cập nhật thông tin nhân viên (UpdateStaffRequest).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStaffRequest {

    // Tên của nhân viên - bắt buộc nhập
    @NotBlank(message = "Tên nhân viên không được để trống")
    private String name;

    // Đường dẫn ảnh đại diện
    private String avatarUrl;

    // Tiểu sử/mô tả của nhân viên
    private String bio;

    // Danh sách chuyên môn hoặc tag kỹ năng (phân tách bởi dấu phẩy)
    private String specialties;

    // Danh sách ID các dịch vụ được phép thực hiện
    private List<Long> serviceIds;

    private String roleCode;
}


