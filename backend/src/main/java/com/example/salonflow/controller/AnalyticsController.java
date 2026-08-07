package com.example.salonflow.controller;

import com.example.salonflow.dto.analytics.SalonOverviewAnalyticsResponse;
import com.example.salonflow.services.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
