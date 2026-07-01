package com.example.salonflow.dto.shift;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Slot khả dụng của 1 staff trong 1 ngày.
 * API trả về danh sách slot này để frontend booking calendar
 * hiển thị khung giờ có thể đặt lịch.
 */
@Data
@Builder
public class AvailabilitySlotResponse {

    private Long userId;
    private String userName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    /** true = slot còn trống, false = đã bị book */
    private boolean available;
}
