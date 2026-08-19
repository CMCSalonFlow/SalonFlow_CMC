package com.example.salonflow.dto.offday;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOffDayResponse {
    private Long id;
    private String title;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Long branchId;
    private String branchName;
    private Boolean isAllBranches;
    private String reason;
    private long totalDays;
    private Instant createdAt;
}
