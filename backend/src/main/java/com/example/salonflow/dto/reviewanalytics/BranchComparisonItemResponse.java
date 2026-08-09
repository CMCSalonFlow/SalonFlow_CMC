package com.example.salonflow.dto.reviewanalytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchComparisonItemResponse {

    private Long branchId;

    private String branchName;

    private BigDecimal averageRating;

    private Long totalReviews;

    /** Star 1..5 -> Count */
    private Map<Integer, Long> ratingDistribution;
}
