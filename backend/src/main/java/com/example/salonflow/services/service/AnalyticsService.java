package com.example.salonflow.services.service;

import com.example.salonflow.dto.analytics.SalonOverviewAnalyticsResponse;

public interface AnalyticsService {
    SalonOverviewAnalyticsResponse getSalonOverviewAnalytics(Long branchId);
}
