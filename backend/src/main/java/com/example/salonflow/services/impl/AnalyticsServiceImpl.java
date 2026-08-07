package com.example.salonflow.services.impl;

import com.example.salonflow.dto.analytics.*;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ReviewRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public SalonOverviewAnalyticsResponse getSalonOverviewAnalytics(Long branchId) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        String branchName = "Tất cả chi nhánh";
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh ID: " + branchId));
            if (!branch.getSalon().getId().equals(salon.getId())) {
                throw new IllegalArgumentException("Chi nhánh không thuộc quyền quản lý của Salon này");
            }
            branchName = branch.getName();
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate last7DaysStart = today.minusDays(6);
        LocalDate pastWeekStart = today.minusDays(7);
        LocalDate pastWeekEnd = today.minusDays(1);

        // Fetch bookings for recent range (past week + today)
        List<Booking> rangeBookings;
        if (branchId != null) {
            rangeBookings = bookingRepository.findByBranchIdAndBookingDateBetween(branchId, pastWeekStart, today);
        } else {
            rangeBookings = bookingRepository.findByBranchSalonIdAndBookingDateBetween(salon.getId(), pastWeekStart, today);
        }

        // Group bookings by date
        Map<LocalDate, List<Booking>> bookingsByDate = rangeBookings.stream()
                .collect(Collectors.groupingBy(Booking::getBookingDate));

        List<Booking> todayBookings = bookingsByDate.getOrDefault(today, List.of());
        List<Booking> yesterdayBookings = bookingsByDate.getOrDefault(yesterday, List.of());

        // 1. Calculate Today KPIs
        BigDecimal todayRevenue = todayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yesterdayRevenue = yesterdayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double revenueGrowthRate = calculateGrowthRate(todayRevenue, yesterdayRevenue);

        long todayBookingsCount = todayBookings.size();
        long yesterdayBookingsCount = yesterdayBookings.size();
        Double bookingsGrowthRate = calculateGrowthRate(
                BigDecimal.valueOf(todayBookingsCount),
                BigDecimal.valueOf(yesterdayBookingsCount)
        );

        long completedCount = todayBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long pendingCount = todayBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long confirmedCount = todayBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long cancelledCount = todayBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        Double completionRate = todayBookingsCount > 0
                ? Math.round(((double) completedCount / todayBookingsCount * 100.0) * 10.0) / 10.0
                : 0.0;

        // Ratings
        Double avgRating;
        Long totalReviews;
        if (branchId != null) {
            avgRating = reviewRepository.calculateAverageRatingByBranchId(branchId);
            totalReviews = reviewRepository.countByBranchId(branchId);
        } else {
            avgRating = reviewRepository.calculateAverageRatingBySalonId(salon.getId());
            totalReviews = reviewRepository.countBySalonId(salon.getId());
        }
        if (avgRating == null) avgRating = 0.0;
        avgRating = Math.round(avgRating * 10.0) / 10.0;
        if (totalReviews == null) totalReviews = 0L;

        KpiMetricDto kpis = KpiMetricDto.builder()
                .todayRevenue(todayRevenue)
                .revenueGrowthRate(revenueGrowthRate)
                .todayBookingsCount(todayBookingsCount)
                .bookingsGrowthRate(bookingsGrowthRate)
                .completionRate(completionRate)
                .completedBookingsCount(completedCount)
                .pendingBookingsCount(pendingCount)
                .confirmedBookingsCount(confirmedCount)
                .cancelledBookingsCount(cancelledCount)
                .averageRating(avgRating)
                .totalReviewCount(totalReviews)
                .build();

        // 2. 7-Day Sparkline Trend
        List<DailyTrendDto> last7DaysTrend = new ArrayList<>();
        for (LocalDate d = last7DaysStart; !d.isAfter(today); d = d.plusDays(1)) {
            List<Booking> dayList = bookingsByDate.getOrDefault(d, List.of());
            BigDecimal dayRevenue = dayList.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long dayTotalBookings = dayList.size();
            long dayCompletedBookings = dayList.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

            last7DaysTrend.add(DailyTrendDto.builder()
                    .date(d)
                    .dayOfWeek(formatDayOfWeek(d.getDayOfWeek()))
                    .revenue(dayRevenue)
                    .bookingCount(dayTotalBookings)
                    .completedCount(dayCompletedBookings)
                    .build());
        }

        // 3. Revenue Alert Calculation
        // Past 7 days (excluding today)
        BigDecimal totalPast7DaysRevenue = BigDecimal.ZERO;
        for (LocalDate d = pastWeekStart; !d.isAfter(pastWeekEnd); d = d.plusDays(1)) {
            List<Booking> dayList = bookingsByDate.getOrDefault(d, List.of());
            BigDecimal rev = dayList.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalPast7DaysRevenue = totalPast7DaysRevenue.add(rev);
        }

        BigDecimal lastWeekDailyAverage = totalPast7DaysRevenue.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
        BigDecimal threshold = lastWeekDailyAverage.multiply(BigDecimal.valueOf(0.80));

        boolean isAlerting = lastWeekDailyAverage.compareTo(BigDecimal.ZERO) > 0
                && todayRevenue.compareTo(threshold) < 0;

        Double dropPercentage = 0.0;
        if (lastWeekDailyAverage.compareTo(BigDecimal.ZERO) > 0 && isAlerting) {
            BigDecimal diff = lastWeekDailyAverage.subtract(todayRevenue);
            dropPercentage = diff.divide(lastWeekDailyAverage, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            dropPercentage = Math.round(dropPercentage * 10.0) / 10.0;
        }

        String alertMessage = null;
        if (isAlerting) {
            alertMessage = String.format(
                    "Doanh thu hôm nay (%,d VNĐ) đang thấp hơn 80%% so với trung bình 7 ngày tuần trước (%,d VNĐ/ngày). Giảm %,.1f%%.",
                    todayRevenue.longValue(),
                    lastWeekDailyAverage.longValue(),
                    dropPercentage
            );
        }

        RevenueAlertDto revenueAlert = RevenueAlertDto.builder()
                .isAlerting(isAlerting)
                .todayRevenue(todayRevenue)
                .lastWeekDailyAverage(lastWeekDailyAverage)
                .thresholdPercentage(80.0)
                .dropPercentage(dropPercentage)
                .message(alertMessage)
                .build();

        return SalonOverviewAnalyticsResponse.builder()
                .salonId(salon.getId())
                .salonName(salon.getName())
                .branchId(branchId)
                .branchName(branchName)
                .kpis(kpis)
                .last7DaysTrend(last7DaysTrend)
                .revenueAlert(revenueAlert)
                .build();
    }

    private Double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        BigDecimal diff = current.subtract(previous);
        double rate = diff.divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
        return Math.round(rate * 10.0) / 10.0;
    }

    private String formatDayOfWeek(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "CN";
        };
    }
}
