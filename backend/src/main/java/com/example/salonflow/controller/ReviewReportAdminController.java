package com.example.salonflow.controller;

import com.example.salonflow.dto.review.ResolveReviewReportRequest;
import com.example.salonflow.dto.review.ReviewReportResponse;
import com.example.salonflow.entity.enums.ReviewReportStatus;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.ReviewAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/review-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReviewReportAdminController {

    private final ReviewAdminService reviewAdminService;

    @GetMapping
    public ResponseEntity<Page<ReviewReportResponse>> getReviewReports(
            @RequestParam(required = false) ReviewReportStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Page<ReviewReportResponse> response = reviewAdminService.getReviewReports(
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ReviewReportResponse> resolveReport(
            @PathVariable("id") Long reportId,
            @Valid @RequestBody ResolveReviewReportRequest request
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        ReviewReportResponse response = reviewAdminService.resolveReport(reportId, request, adminId);
        return ResponseEntity.ok(response);
    }
}
