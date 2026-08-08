package com.example.salonflow.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeakPeriodDto {
    private String label;
    private LocalDate date;
    private BigDecimal revenue;
    private Long bookingCount;
}
