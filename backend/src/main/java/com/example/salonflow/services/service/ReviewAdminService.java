package com.example.salonflow.services.service;

import com.example.salonflow.dto.review.ReviewAdminDetailResponse;
import com.example.salonflow.dto.review.ReviewPageResponse;
import com.example.salonflow.dto.review.ReviewSentimentSummaryResponse;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import org.springframework.data.domain.Pageable;

public interface ReviewAdminService {

    ReviewPageResponse search(Long branchId, String sentiment, ReviewSentimentStatus status, String keyword, Pageable pageable);

    ReviewAdminDetailResponse getById(Long reviewId);

    ReviewSentimentSummaryResponse summary(Long branchId);
}
