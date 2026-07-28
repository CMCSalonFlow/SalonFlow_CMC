package com.example.salonflow.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAdminItemResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long bookingId;
    private Long branchId;
    private String branchName;
    private Long staffId;
    private String staffName;
    private Integer rating;
    private String title;
    private String content;
    private String sentiment;
    private BigDecimal sentimentConfidence;
    private String sentimentStatus;
    private String sentimentProvider;
    private Instant sentimentAnalyzedAt;
    private String sentimentError;
    private Instant createdAt;
    private Instant updatedAt;
    private String sentimentBadgeColor;
}
