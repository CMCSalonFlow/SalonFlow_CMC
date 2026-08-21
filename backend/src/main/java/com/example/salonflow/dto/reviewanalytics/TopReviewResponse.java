package com.example.salonflow.dto.reviewanalytics;

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
public class TopReviewResponse {

    private Long id;

    private String customerName;

    private String customerAvatar;

    private Integer rating;

    private String title;

    private String comment;

    private String sentiment;

    private BigDecimal sentimentConfidence;

    private String branchName;

    private Instant createdAt;
}