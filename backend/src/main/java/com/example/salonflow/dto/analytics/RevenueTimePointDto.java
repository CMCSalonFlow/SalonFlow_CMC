package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueTimePointDto {
    private String label; // VD: "01/08", "Tuần 32", "Tháng 08/2026", "Năm 2026"
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal currentRevenue;
    private BigDecimal previousYearRevenue; // Doanh thu cùng kỳ năm ngoái
    private Double yoyGrowthRate; // % tăng trưởng YoY

    private Long bookingCount;
    private Boolean isPeakPeriod; // true nếu là mốc doanh thu cao nhất
}
