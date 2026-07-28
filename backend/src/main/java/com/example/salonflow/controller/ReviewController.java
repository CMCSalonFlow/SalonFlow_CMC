package com.example.salonflow.controller;

import com.example.salonflow.dto.review.*;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * API POST /api/v1/bookings/{id}/reviews
     * Tạo đánh giá cho đơn đặt lịch đã hoàn thành.
     */
    @PostMapping("/bookings/{id}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable("id") Long bookingId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.createReview(bookingId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API GET /api/v1/bookings/{id}/reviews
     * Lấy chi tiết bài đánh giá của một booking.
     */
    @GetMapping("/bookings/{id}/reviews")
    public ResponseEntity<ReviewResponse> getReviewByBookingId(@PathVariable("id") Long bookingId) {
        ReviewResponse response = reviewService.getReviewByBookingId(bookingId);
        return ResponseEntity.ok(response);
    }

    /**
     * API GET /api/v1/salons/{salonId}/reviews
     * Lấy danh sách đánh giá của Salon (Phân trang).
     */
    @GetMapping("/salons/{salonId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviewsBySalonId(
            @PathVariable("salonId") Long salonId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewResponse> page = reviewService.getReviewsBySalonId(salonId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * API GET /api/v1/salons/{salonId}/review-summary
     * Lấy thống kê điểm số trung bình và phân bố sao của Salon.
     */
    @GetMapping("/salons/{salonId}/review-summary")
    public ResponseEntity<SalonRatingSummaryResponse> getSalonReviewSummary(
            @PathVariable("salonId") Long salonId
    ) {
        SalonRatingSummaryResponse summary = reviewService.getSalonReviewSummary(salonId);
        return ResponseEntity.ok(summary);
    }

    /**
     * API GET /api/v1/branches/{branchId}/reviews
     * Lấy danh sách đánh giá của Chi nhánh.
     */
    @GetMapping("/branches/{branchId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByBranchId(
            @PathVariable("branchId") Long branchId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewResponse> page = reviewService.getReviewsByBranchId(branchId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * API GET /api/v1/branches/{branchId}/review-summary
     * Lấy thống kê điểm số trung bình và phân bố sao của Chi nhánh.
     */
    @GetMapping("/branches/{branchId}/review-summary")
    public ResponseEntity<BranchRatingSummaryResponse> getBranchReviewSummary(
            @PathVariable("branchId") Long branchId
    ) {
        BranchRatingSummaryResponse summary = reviewService.getBranchReviewSummary(branchId);
        return ResponseEntity.ok(summary);
    }

    /**
     * API POST /api/v1/reviews/{id}/reply
     * Salon Owner phản hồi đánh giá (1 reply per review).
     */
    @PostMapping("/reviews/{id}/reply")
    public ResponseEntity<ReviewResponse> replyReview(
            @PathVariable("id") Long reviewId,
            @Valid @RequestBody OwnerReplyReviewRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.replyReview(reviewId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * API POST /api/v1/reviews/{id}/report
     * Báo cáo đánh giá vi phạm.
     */
    @PostMapping("/reviews/{id}/report")
    public ResponseEntity<ReviewReportResponse> reportReview(
            @PathVariable("id") Long reviewId,
            @Valid @RequestBody ReportReviewRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ReviewReportResponse response = reviewService.reportReview(reviewId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
