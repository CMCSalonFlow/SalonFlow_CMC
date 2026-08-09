package com.example.salonflow.dto.reviewanalytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingTrendPointResponse {

    /** Định dạng "YYYY-MM", ví dụ "2026-06" */
    private String month;

    private BigDecimal averageRating;

    private Long totalReviews;
}
