package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonOverviewAnalyticsResponse {
    private Long salonId;
    private String salonName;
    private Long branchId; // null nếu là Tất cả chi nhánh
    private String branchName;
    private KpiMetricDto kpis;
    private List<DailyTrendDto> last7DaysTrend;
    private RevenueAlertDto revenueAlert;
}
