package com.example.salonflow.dto.booking;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

/**
 * Phản hồi danh sách các khung giờ trống khả dụng (AvailabilityResponse) để đặt lịch trên giao diện.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {

    // Danh sách các mốc thời gian (giờ bắt đầu) khả dụng trong ngày
    private List<LocalTime> availableStartTimes;

    // Giờ mở cửa của chi nhánh trong ngày được chọn
    private LocalTime openTime;

    // Giờ đóng cửa của chi nhánh trong ngày được chọn
    private LocalTime closeTime;
}
