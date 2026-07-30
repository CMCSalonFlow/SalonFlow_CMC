package com.example.salonflow.ai.controller;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.service.ReviewSentimentService;
import com.example.salonflow.dto.common.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/reviews/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
public class OwnerReviewAiController {

    private final ReviewSentimentService reviewSentimentService;
    private final AiProperties aiProperties;

    @PostMapping("/trigger")
    public ResponseEntity<MessageResponse> triggerReviewSentiment() {
        if (!aiProperties.isEnabled() || !aiProperties.getReview().isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(MessageResponse.builder()
                            .message("AI review processing is currently disabled")
                            .build());
        }

        reviewSentimentService.enqueuePendingReviews();
        return ResponseEntity.accepted()
                .body(MessageResponse.builder()
                        .message("AI review sentiment job has been triggered")
                        .build());
    }
}
