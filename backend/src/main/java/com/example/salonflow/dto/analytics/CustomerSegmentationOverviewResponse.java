package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSegmentationOverviewResponse {
    private Long salonId;
    private Long branchId;
    private String branchName;

    private Long totalCustomers;

    // Segment Counts & Percentages
    private Long newCount;
    private Double newPercentage;

    private Long returningCount;
    private Double returningPercentage;

    private Long vipCount;
    private Double vipPercentage;

    private Long atRiskCount;
    private Double atRiskPercentage;

    // Metrics
    private BigDecimal averageOrderValue; // AOV
    private Double averageFrequencyPerMonth; // Frequency
    private BigDecimal averageCustomerLifetimeValue; // Avg CLV
}
