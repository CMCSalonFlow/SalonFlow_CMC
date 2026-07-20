package com.example.salonflow.dto.loyalty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltySummaryResponse {
    private Integer totalPoints;
    private Integer expiringPoints;
    private String memberRank;
}
