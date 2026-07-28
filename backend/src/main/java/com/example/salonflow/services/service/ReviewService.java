package com.example.salonflow.services.service;

import com.example.salonflow.dto.review.BranchRatingSummaryResponse;
import com.example.salonflow.dto.review.CreateReviewRequest;
import com.example.salonflow.dto.review.ReviewResponse;
import com.example.salonflow.dto.review.SalonRatingSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(Long bookingId, CreateReviewRequest request, Long currentUserId);

    ReviewResponse getReviewByBookingId(Long bookingId);

    Page<ReviewResponse> getReviewsBySalonId(Long salonId, Pageable pageable);

    Page<ReviewResponse> getReviewsByBranchId(Long branchId, Pageable pageable);

    SalonRatingSummaryResponse getSalonReviewSummary(Long salonId);

    BranchRatingSummaryResponse getBranchReviewSummary(Long branchId);
}
