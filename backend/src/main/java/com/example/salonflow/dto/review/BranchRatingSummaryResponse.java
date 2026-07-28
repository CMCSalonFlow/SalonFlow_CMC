package com.example.salonflow.dto.review;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchRatingSummaryResponse {

    private Long branchId;

    private BigDecimal averageRating;

    private Long totalReviews;

    private Map<Integer, Long> ratingDistribution; // Star 1..5 -> Count
}
