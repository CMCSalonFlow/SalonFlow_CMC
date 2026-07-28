package com.example.salonflow.controller;

import com.example.salonflow.dto.review.ReviewAdminDetailResponse;
import com.example.salonflow.dto.review.ReviewPageResponse;
import com.example.salonflow.dto.review.ReviewSentimentSummaryResponse;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import com.example.salonflow.services.service.ReviewAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class ReviewAdminController {

    private final ReviewAdminService reviewAdminService;

    @GetMapping
    public ResponseEntity<ReviewPageResponse> search(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) ReviewSentimentStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ResponseEntity.ok(
                reviewAdminService.search(
                        branchId,
                        sentiment,
                        status,
                        q,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
        );
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewAdminDetailResponse> getById(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewAdminService.getById(reviewId));
    }

    @GetMapping("/summary")
    public ResponseEntity<ReviewSentimentSummaryResponse> summary(
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(reviewAdminService.summary(branchId));
    }
}
