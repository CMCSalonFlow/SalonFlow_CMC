package com.example.salonflow.dto.staff;

import com.example.salonflow.dto.service.ServiceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Phản hồi public cho danh sách nhân viên ở bước chọn staff.
 * Trả về thông tin cơ bản kèm danh sách kỹ năng dịch vụ và userId liên kết.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicStaffResponse {

    private Long id;

    private Long branchId;

    private String name;

    private String avatarUrl;

    private String bio;

    private String specialties;

    private List<ServiceResponse> services;

    private Long userId;

    private String roleCode;

    private String roleName;
}
