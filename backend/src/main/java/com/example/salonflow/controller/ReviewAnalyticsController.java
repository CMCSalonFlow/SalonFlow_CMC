package com.example.salonflow.controller;

import com.example.salonflow.dto.reviewanalytics.BranchComparisonResponse;
import com.example.salonflow.dto.reviewanalytics.RatingTrendResponse;
import com.example.salonflow.dto.reviewanalytics.TopReviewListResponse;
import com.example.salonflow.dto.reviewanalytics.WordCloudResponse;
import com.example.salonflow.services.service.ReviewAnalyticsService;
import com.example.salonflow.services.service.ReviewKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.salonflow.exception.BadRequestException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * US-045: Dashboard tổng hợp rating - API analytics.
 * Không đụng tới ReviewController/ReviewService CRUD hiện có (thuộc feature/rating).
 */
@RestController
@RequestMapping("/api/v1/analytics/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('SALON_OWNER')")
public class ReviewAnalyticsController {
    // Check chi tiết "Salon Owner chỉ được xem đúng salon/branch của mình" nằm ở
    // ReviewAnalyticsServiceImpl/ReviewKeywordServiceImpl (không thể check bằng @PreAuthorize
    // vì cần đọc dữ liệu salon/branch trong DB, không chỉ dựa vào role).

    private final ReviewAnalyticsService reviewAnalyticsService;
    private final ReviewKeywordService reviewKeywordService;

    /**
     * GET /api/v1/analytics/reviews/trend?salonId=&branchId=&fromMonth=2026-01&toMonth=2026-06
     * Trend rating trung bình theo tháng (line chart).
     */
    @GetMapping("/trend")
    public ResponseEntity<RatingTrendResponse> getRatingTrend(
            @RequestParam(required = false) Long salonId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String fromMonth, // "YYYY-MM"
            @RequestParam(required = false) String toMonth    // "YYYY-MM"
    ) {
        YearMonth from = parseYearMonth(fromMonth, "fromMonth");
        YearMonth to = parseYearMonth(toMonth, "toMonth");
        return ResponseEntity.ok(reviewAnalyticsService.getRatingTrend(salonId, branchId, from, to));
    }

    private YearMonth parseYearMonth(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(fieldName + " phải theo định dạng YYYY-MM, ví dụ 2026-06.");
        }
    }

    /**
     * GET /api/v1/analytics/reviews/top?salonId=&branchId=&limit=5
     * Top review tích cực / tiêu cực nhất.
     */
    @GetMapping("/top")
    public ResponseEntity<TopReviewListResponse> getTopReviews(
            @RequestParam(required = false) Long salonId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(reviewAnalyticsService.getTopReviews(salonId, branchId, limit));
    }

    /**
     * GET /api/v1/analytics/reviews/compare-branches?salonId=
     * So sánh rating giữa các chi nhánh trong 1 salon.
     */
    @GetMapping("/compare-branches")
    public ResponseEntity<BranchComparisonResponse> compareBranches(
            @RequestParam Long salonId
    ) {
        return ResponseEntity.ok(reviewAnalyticsService.compareBranches(salonId));
    }

    /**
     * GET /api/v1/analytics/reviews/export?salonId=&branchId=
     * Export review ra file CSV.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReviewsCsv(
            @RequestParam(required = false) Long salonId,
            @RequestParam(required = false) Long branchId
    ) {
        byte[] csvBytes = reviewAnalyticsService.exportReviewsCsv(salonId, branchId);

        String filename = "reviews-export-" + (branchId != null ? "branch-" + branchId : "salon-" + salonId) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    /**
     * GET /api/v1/analytics/reviews/keywords?salonId=&branchId=&yearMonth=2026-07&limit=30
     * Word cloud - dữ liệu ĐÃ TÍNH SẴN bởi batch job hàng ngày (KHÔNG tính real-time).
     */
    @GetMapping("/keywords")
    public ResponseEntity<WordCloudResponse> getWordCloud(
            @RequestParam(required = false) Long salonId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String yearMonth, // "YYYY-MM"
            @RequestParam(defaultValue = "30") int limit
    ) {
        YearMonth month = parseYearMonth(yearMonth, "yearMonth");
        return ResponseEntity.ok(reviewKeywordService.getWordCloud(salonId, branchId, month, limit));
    }
}