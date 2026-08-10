package com.example.salonflow.dto.analytics;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffPerformanceResponse {

    private String period;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Long branchId;
    private String branchName;

    private List<StaffPerformanceMetricsDto> top3Performers;
    private List<StaffLowRatingWarningDto> lowRatingWarnings;
    private List<StaffPerformanceMetricsDto> staffPerformanceList;
}
