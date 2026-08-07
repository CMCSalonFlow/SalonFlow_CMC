package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalyticsResponse {
    private String period; // "daily", "weekly", "monthly", "yearly"
    private LocalDate fromDate;
    private LocalDate toDate;
    private Long salonId;
    private Long branchId;

    private BigDecimal totalRevenue;
    private BigDecimal totalPreviousYearRevenue;
    private Double overallYoYGrowthRate; // % tăng trưởng YoY tổng thể

    private PeakPeriodDto peakPeriod; // Highlights mốc doanh thu cao nhất
    private List<RevenueTimePointDto> timeline;
    private List<ServiceRevenueBreakdownDto> serviceBreakdown;
}
