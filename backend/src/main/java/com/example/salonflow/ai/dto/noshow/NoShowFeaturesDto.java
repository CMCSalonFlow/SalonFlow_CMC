package com.example.salonflow.ai.dto.noshow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoShowFeaturesDto {

    // 1. Lịch sử hủy/no-show (tỷ lệ cancel_rate trong khoảng 0.0 - 1.0)
    private Double cancelRate;
    private Long totalPastBookings;
    private Long totalCancelledOrNoShowBookings;

    // 2. Khoảng cách địa lý (tính bằng km và giá trị chuẩn hóa [0, 1])
    private Double distanceKm;
    private Double distanceNorm;

    // 3. Lead time đặt lịch trước (tính bằng giờ và giá trị chuẩn hóa [0, 1])
    private Double leadTimeHours;
    private Double leadTimeNorm;

    // 4. Số lần đã đến và hoàn thành dịch vụ thành công trong quá khứ
    private Long completedCount;
    private Double completedCountNorm;
}
