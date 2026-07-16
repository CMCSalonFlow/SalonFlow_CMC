package com.example.salonflow.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phản hồi public cho danh sách nhân viên ở bước chọn staff.
 * Không trả về email, phone, userId để tránh lộ dữ liệu nội bộ.
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
}
