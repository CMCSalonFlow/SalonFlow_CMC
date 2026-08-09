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
public class CustomerSegmentDetailDto {
    private Long customerId;
    private String fullName;
    private String phone;
    private String avatarUrl;

    private String segmentType; // 'NEW', 'RETURNING', 'VIP', 'AT_RISK'
    private Long completedBookingsCount;
    private BigDecimal totalSpent;

    private LocalDate firstBookingDate;
    private LocalDate lastBookingDate;
    private Long daysSinceLastBooking;

    private BigDecimal averageOrderValue; // AOV
    private Double frequencyPerMonth;      // Tần suất (lần/tháng)
    private BigDecimal customerLifetimeValue; // CLV
}
