package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeakHourCellDto {
    private int dayOfWeek;       // 1 (Monday) to 7 (Sunday)
    private String dayName;      // "Thứ 2", "Thứ 3", ..., "Chủ Nhật"
    private int hourOfDay;      // 0 to 23
    private int minuteOfHour;   // 0 or 30
    private String hourLabel;    // "07:00", "07:30", "08:00", ...
    private long bookingCount;   // Số lượng lượt đặt lịch trong ô này
    private double intensity;    // Tỷ lệ mật độ từ 0.0 đến 1.0
}
