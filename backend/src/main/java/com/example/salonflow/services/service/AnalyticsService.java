package com.example.salonflow.services.service;

import com.example.salonflow.dto.analytics.PeakHourHeatmapResponse;
import com.example.salonflow.dto.analytics.RevenueAnalyticsResponse;
import com.example.salonflow.dto.analytics.SalonOverviewAnalyticsResponse;

import java.time.LocalDate;

public interface AnalyticsService {
    SalonOverviewAnalyticsResponse getSalonOverviewAnalytics(Long branchId);

    RevenueAnalyticsResponse getSalonRevenueAnalytics(String period, LocalDate fromDate, LocalDate toDate, Long branchId);

    PeakHourHeatmapResponse getPeakHourHeatmap(Long branchId, LocalDate fromDate, LocalDate toDate);
}
