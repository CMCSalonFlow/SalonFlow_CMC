package com.example.salonflow.ai.dto.scheduling;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSchedulingRequest {

    @NotNull(message = "Chi nhánh không được để trống")
    private Long branchId;

    @NotNull(message = "Ngày đặt lịch không được để trống")
    private LocalDate date;

    private List<Long> serviceIds;

    private Long bundleId;

    private Long preferredStaffId;

    // Khoảng giờ mong muốn (nếu có)
    private LocalTime preferredStartTime;
    private LocalTime preferredEndTime;

    // Vị trí khách hàng (nếu có để hỗ trợ tối ưu di chuyển)
    private Double customerLatitude;
    private Double customerLongitude;
}
