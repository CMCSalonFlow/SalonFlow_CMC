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
public class DailyTrendDto {
    private LocalDate date;
    private String dayOfWeek;
    private BigDecimal revenue;
    private Long bookingCount;
    private Long completedCount;
}
