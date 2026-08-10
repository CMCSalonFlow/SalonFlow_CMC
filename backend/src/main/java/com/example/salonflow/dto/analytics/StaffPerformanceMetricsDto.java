package com.example.salonflow.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffPerformanceMetricsDto {

    private Long staffId;
    private String staffName;
    private String avatarUrl;
    private String specialties;
    private Long branchId;
    private String branchName;

    private Long completedBookings;
    private BigDecimal totalRevenue;
    private Double avgRating;
    private Long totalReviewsCount;
    private Long totalWorkingShifts;
    private Long bookedSlotsCount;
    private Long totalAvailableSlots;
    private Double slotOccupancyRate; // Phần trăm lấp đầy slot %

    private Integer revenueRank;
    private Integer bookingRank;
    private Integer ratingRank;
    private Integer overallRank;
}
