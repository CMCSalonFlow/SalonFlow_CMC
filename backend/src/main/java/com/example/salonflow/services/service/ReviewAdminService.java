package com.example.salonflow.services.service;

import com.example.salonflow.dto.review.ReviewAdminDetailResponse;
import com.example.salonflow.dto.review.ReviewPageResponse;
import com.example.salonflow.dto.review.ReviewSentimentSummaryResponse;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import com.example.salonflow.dto.review.ReviewReportResponse;
import com.example.salonflow.dto.review.ResolveReviewReportRequest;
import com.example.salonflow.entity.enums.ReviewReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewAdminService {

    ReviewPageResponse search(Long branchId, String sentiment, ReviewSentimentStatus status, String keyword, Pageable pageable);

    ReviewAdminDetailResponse getById(Long reviewId);

    ReviewSentimentSummaryResponse summary(Long branchId);

    Page<ReviewReportResponse> getReviewReports(ReviewReportStatus status, Pageable pageable);

    ReviewReportResponse resolveReport(Long reportId, ResolveReviewReportRequest request, Long adminId);
}
