package com.example.salonflow.dto.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueForecastResponse {
    private Long branchId;
    private String modelKey;
    private Integer months;
    private Integer periods;
    private LocalDate historyStartDate;
    private LocalDate historyEndDate;
    private List<DailyRevenuePoint> actuals;
    private List<ForecastPoint> forecast;
}
