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
public class RevenueAlertDto {
    private Boolean isAlerting;
    private BigDecimal todayRevenue;
    private BigDecimal lastWeekDailyAverage;
    private Double thresholdPercentage; // Mặc định 80.0%
    private Double dropPercentage; // % giảm thực tế
    private String message;
}
