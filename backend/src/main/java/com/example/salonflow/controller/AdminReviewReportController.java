package com.example.salonflow.controller;

import com.example.salonflow.dto.review.ResolveReviewReportRequest;
import com.example.salonflow.dto.review.ReviewReportResponse;
import com.example.salonflow.entity.enums.ReviewReportStatus;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/review-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminReviewReportController {

    private final ReviewService reviewService;

    /**
     * API GET /api/v1/admin/review-reports
     * Lấy hàng đợi báo cáo đánh giá vi phạm dành cho Admin.
     */
    @GetMapping
    public ResponseEntity<Page<ReviewReportResponse>> getReviewReports(
            @RequestParam(name = "status", required = false, defaultValue = "PENDING") ReviewReportStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewReportResponse> page = reviewService.getReviewReports(status, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * API PUT /api/v1/admin/review-reports/{id}/approve
     * Chấp nhận báo cáo vi phạm $\rightarrow$ Ẩn đánh giá khỏi public & gửi email thông báo 2 bên.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ReviewReportResponse> approveReport(
            @PathVariable("id") Long reportId,
            @RequestBody(required = false) ResolveReviewReportRequest request
    ) {
        Long adminUserId = SecurityUtils.getCurrentUserId();
        ReviewReportResponse response = reviewService.approveReport(reportId, request, adminUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * API PUT /api/v1/admin/review-reports/{id}/reject
     * Từ chối báo cáo vi phạm $\rightarrow$ Giữ bài đánh giá & gửi email cho người báo cáo.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ReviewReportResponse> rejectReport(
            @PathVariable("id") Long reportId,
            @RequestBody(required = false) ResolveReviewReportRequest request
    ) {
        Long adminUserId = SecurityUtils.getCurrentUserId();
        ReviewReportResponse response = reviewService.rejectReport(reportId, request, adminUserId);
        return ResponseEntity.ok(response);
    }
}
