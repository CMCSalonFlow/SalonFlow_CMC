package com.example.salonflow.services.service;

import com.example.salonflow.dto.reviewanalytics.BranchComparisonResponse;
import com.example.salonflow.dto.reviewanalytics.RatingTrendResponse;
import com.example.salonflow.dto.reviewanalytics.TopReviewListResponse;

import java.time.YearMonth;

public interface ReviewAnalyticsService {

    /**
     * Trend rating trung bình theo tháng.
     * Chỉ 1 trong 2 tham số salonId / branchId được truyền (branchId ưu tiên nếu có cả 2).
     */
    RatingTrendResponse getRatingTrend(Long salonId, Long branchId, YearMonth fromMonth, YearMonth toMonth);

    /**
     * Top N review tích cực & tiêu cực nhất.
     * Chỉ 1 trong 2 tham số salonId / branchId được truyền (branchId ưu tiên nếu có cả 2).
     */
    TopReviewListResponse getTopReviews(Long salonId, Long branchId, int limit);

    /** So sánh rating giữa các chi nhánh trong 1 salon. */
    BranchComparisonResponse compareBranches(Long salonId);

    /**
     * Export review ra CSV (UTF-8 BOM để Excel đọc đúng tiếng Việt).
     * Chỉ 1 trong 2 tham số salonId / branchId được truyền (branchId ưu tiên nếu có cả 2).
     */
    byte[] exportReviewsCsv(Long salonId, Long branchId);
}
