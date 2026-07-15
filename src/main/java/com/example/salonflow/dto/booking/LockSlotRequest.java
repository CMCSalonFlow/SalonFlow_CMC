package com.example.salonflow.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request lock slot khi user chọn khung giờ.
 * Backend dùng Redis SETNX để lock slot trong 10 phút.
 */
@Data
public class LockSlotRequest {

    @NotNull(message = "Branch không được để trống")
    private Long branchId;

    @NotNull(message = "Staff không được để trống")
    private Long staffId;

    @NotNull(message = "Service không được để trống")
    private Long serviceId;

    @NotNull(message = "Ngày đặt không được để trống")
    private LocalDate bookingDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;
}
