package com.example.salonflow.services.service;

import com.example.salonflow.dto.review.*;
import com.example.salonflow.entity.enums.ReviewReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(Long bookingId, CreateReviewRequest request, Long currentUserId);

    ReviewResponse getReviewByBookingId(Long bookingId);

    Page<ReviewResponse> getReviewsBySalonId(Long salonId, Integer rating, Pageable pageable);

    Page<ReviewResponse> getReviewsByBranchId(Long branchId, Pageable pageable);

    SalonRatingSummaryResponse getSalonReviewSummary(Long salonId);

    BranchRatingSummaryResponse getBranchReviewSummary(Long branchId);

    // Salon Owner phản hồi đánh giá (Giới hạn 1 phản hồi / review)
    ReviewResponse replyReview(Long reviewId, OwnerReplyReviewRequest request, Long currentUserId);

    // Khách hàng / Owner báo cáo đánh giá vi phạm
    ReviewReportResponse reportReview(Long reviewId, ReportReviewRequest request, Long currentUserId);

    // Admin lấy danh sách hàng đợi báo cáo vi phạm
    Page<ReviewReportResponse> getReviewReports(ReviewReportStatus status, Pageable pageable);

    // Admin duyệt chấp nhận báo cáo vi phạm (Ẩn bài & Gửi email)
    ReviewReportResponse approveReport(Long reportId, ResolveReviewReportRequest request, Long adminUserId);

    // Admin từ chối báo cáo vi phạm (Gửi email)
    ReviewReportResponse rejectReport(Long reportId, ResolveReviewReportRequest request, Long adminUserId);
}
