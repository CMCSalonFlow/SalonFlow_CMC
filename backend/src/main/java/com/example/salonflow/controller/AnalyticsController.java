package com.example.salonflow.controller;

import com.example.salonflow.dto.analytics.PeakHourHeatmapResponse;
import com.example.salonflow.dto.analytics.RevenueAnalyticsResponse;
import com.example.salonflow.dto.analytics.SalonOverviewAnalyticsResponse;
import com.example.salonflow.services.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/owner/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Lấy dữ liệu Dashboard tổng quan salon (KPI cards, mini 7-day sparklines, cảnh báo doanh thu)
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public SalonOverviewAnalyticsResponse getOverviewAnalytics(
            @RequestParam(required = false) Long branchId
    ) {
        return analyticsService.getSalonOverviewAnalytics(branchId);
    }

    /**
     * Lấy dữ liệu Phân tích Doanh thu chi tiết theo ngày/tuần/tháng/năm
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public RevenueAnalyticsResponse getRevenueAnalytics(
            @RequestParam(required = false, defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long branchId
    ) {
        return analyticsService.getSalonRevenueAnalytics(period, from, to, branchId);
    }

    /**
     * Lấy dữ liệu Biểu đồ Heatmap Khung giờ Cao điểm theo ngày trong tuần (7x15 grid)
     */
    @GetMapping("/peak-hours")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public PeakHourHeatmapResponse getPeakHourHeatmap(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return analyticsService.getPeakHourHeatmap(branchId, from, to);
    }
}
