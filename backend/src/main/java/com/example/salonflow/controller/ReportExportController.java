package com.example.salonflow.controller;

import com.example.salonflow.dto.analytics.RevenueAnalyticsResponse;
import com.example.salonflow.dto.analytics.StaffPerformanceResponse;
import com.example.salonflow.scheduler.WeeklyReportScheduler;
import com.example.salonflow.services.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/owner/reports")
@RequiredArgsConstructor
public class ReportExportController {

    private final AnalyticsService analyticsService;
    private final WeeklyReportScheduler weeklyReportScheduler;

    /**
     * API Lấy dữ liệu Báo cáo tổng hợp (Doanh thu / Nhân viên / Dịch vụ) theo khoảng thời gian tùy chọn
     */
    @GetMapping("/data")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<Map<String, Object>> getReportData(
            @RequestParam(required = false, defaultValue = "doanh_thu") String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long branchId
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("reportType", reportType);
        result.put("fromDate", from != null ? from : LocalDate.now().minusDays(30));
        result.put("toDate", to != null ? to : LocalDate.now());
        result.put("branchId", branchId);

        if ("nhan_vien".equalsIgnoreCase(reportType)) {
            StaffPerformanceResponse staffData = analyticsService.getStaffPerformanceReport("custom", from, to, branchId);
            result.put("details", staffData);
        } else if ("dich_vu".equalsIgnoreCase(reportType)) {
            RevenueAnalyticsResponse revenueData = analyticsService.getSalonRevenueAnalytics("daily", from, to, branchId);
            result.put("details", revenueData);
        } else {
            // Mặc định: Doanh thu
            RevenueAnalyticsResponse revenueData = analyticsService.getSalonRevenueAnalytics("daily", from, to, branchId);
            result.put("details", revenueData);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * API Kích hoạt thử nghiệm gửi Email Báo Cáo Tuần (Mô phỏng Job 8h Sáng Thứ 2)
     */
    @PostMapping("/trigger-weekly-email")
    @PreAuthorize("hasRole('SALON_OWNER') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<Map<String, String>> triggerWeeklyReportEmail() {
        weeklyReportScheduler.sendWeeklyReportEmailToOwners();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đã kích hoạt gửi Email Báo Cáo Tuần (Job 8:00 AM Thứ 2) tới tất cả Chủ Salon thành công!");
        return ResponseEntity.ok(response);
    }
}
