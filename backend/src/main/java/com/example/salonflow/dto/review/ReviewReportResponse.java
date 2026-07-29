package com.example.salonflow.dto.review;

import com.example.salonflow.entity.enums.ReviewReportStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportResponse {

    private Long id;
    private Long reviewId;
    private Integer reviewRating;
    private String reviewComment;
    private String reviewAuthorName;
    private String reviewAuthorEmail;
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private String reason;
    private ReviewReportStatus status;
    private String adminNotes;
    private Long resolvedById;
    private String resolvedByName;
    private Instant resolvedAt;
    private Instant createdAt;
}
