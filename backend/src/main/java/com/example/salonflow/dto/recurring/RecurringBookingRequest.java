package com.example.salonflow.dto.recurring;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request dùng chung cho cả 2 API: preview và confirm.
 * Preview: chỉ tính toán, KHÔNG lưu DB.
 * Confirm: thực sự tạo các booking.
 */
@Data
public class RecurringBookingRequest {

    @NotNull(message = "Branch không được để trống")
    private Long branchId;

    @NotNull(message = "Staff không được để trống")
    private Long staffId;

    @NotNull(message = "Service không được để trống")
    private Long serviceId;

    /** "WEEKLY" hoặc "BIWEEKLY" */
    @NotNull(message = "Pattern lặp không được để trống")
    private String pattern;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @FutureOrPresent(message = "Ngày bắt đầu phải từ hôm nay trở đi")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    private String note;
}
