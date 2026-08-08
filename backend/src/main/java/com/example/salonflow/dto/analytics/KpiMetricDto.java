package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiMetricDto {
    private BigDecimal todayRevenue;
    private Double revenueGrowthRate; // Tỷ lệ tăng trưởng so với ngày hôm qua (%)

    private Long todayBookingsCount;
    private Double bookingsGrowthRate; // Tỷ lệ tăng trưởng số lượt đặt (%)

    private Double completionRate; // Tỷ lệ hoàn thành %
    private Long completedBookingsCount;
    private Long pendingBookingsCount;
    private Long confirmedBookingsCount;
    private Long cancelledBookingsCount;

    private Double averageRating;
    private Long totalReviewCount;
}
