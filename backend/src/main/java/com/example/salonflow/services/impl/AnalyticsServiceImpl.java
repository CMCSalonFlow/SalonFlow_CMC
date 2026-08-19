package com.example.salonflow.services.impl;

import com.example.salonflow.dto.analytics.*;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.BookingItem;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BookingItemRepository;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ReviewRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.AnalyticsService;
import com.example.salonflow.services.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ReviewRepository reviewRepository;
    private final com.example.salonflow.repository.StaffRepository staffRepository;
    private final com.example.salonflow.repository.ShiftRepository shiftRepository;
    private final SubscriptionService subscriptionService;

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

        List<Booking> rangeBookings;
        if (branchId != null) {
            rangeBookings = bookingRepository.findByBranchIdAndBookingDateBetween(branchId, pastWeekStart, today);
        } else {
            rangeBookings = bookingRepository.findByBranchSalonIdAndBookingDateBetween(salon.getId(), pastWeekStart, today);
        }

        Map<LocalDate, List<Booking>> bookingsByDate = rangeBookings.stream()
                .collect(Collectors.groupingBy(Booking::getBookingDate));

        List<Booking> todayBookings = bookingsByDate.getOrDefault(today, List.of());
        List<Booking> yesterdayBookings = bookingsByDate.getOrDefault(yesterday, List.of());

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

    @Override
    public RevenueAnalyticsResponse getSalonRevenueAnalytics(String period, LocalDate fromDate, LocalDate toDate, Long branchId) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        subscriptionService.validateAdvancedAnalytics(salon.getId());

        String normalizedPeriod = period != null ? period.toLowerCase().trim() : "daily";
        LocalDate today = LocalDate.now();

        if (toDate == null) toDate = today;
        if (fromDate == null) {
            fromDate = switch (normalizedPeriod) {
                case "weekly" -> toDate.minusWeeks(12);
                case "monthly" -> LocalDate.of(toDate.getYear(), 1, 1);
                case "yearly" -> toDate.minusYears(4);
                default -> toDate.minusDays(30); // "daily"
            };
        }

        // Previous year range for YoY comparison
        LocalDate prevYearFrom = fromDate.minusYears(1);
        LocalDate prevYearTo = toDate.minusYears(1);

        // Fetch bookings for both current and previous year ranges
        List<Booking> currentBookings;
        List<Booking> prevYearBookings;
        if (branchId != null) {
            currentBookings = bookingRepository.findByBranchIdAndBookingDateBetween(branchId, fromDate, toDate);
            prevYearBookings = bookingRepository.findByBranchIdAndBookingDateBetween(branchId, prevYearFrom, prevYearTo);
        } else {
            currentBookings = bookingRepository.findByBranchSalonIdAndBookingDateBetween(salon.getId(), fromDate, toDate);
            prevYearBookings = bookingRepository.findByBranchSalonIdAndBookingDateBetween(salon.getId(), prevYearFrom, prevYearTo);
        }

        Map<LocalDate, List<Booking>> currentByDate = currentBookings.stream()
                .collect(Collectors.groupingBy(Booking::getBookingDate));
        Map<LocalDate, List<Booking>> prevByDate = prevYearBookings.stream()
                .collect(Collectors.groupingBy(Booking::getBookingDate));

        // Group timeline based on period
        List<RevenueTimePointDto> timeline = buildTimeline(normalizedPeriod, fromDate, toDate, currentByDate, prevByDate);

        // Find Peak Period (highest currentRevenue)
        RevenueTimePointDto peakPoint = timeline.stream()
                .max(Comparator.comparing(RevenueTimePointDto::getCurrentRevenue))
                .orElse(null);

        PeakPeriodDto peakPeriod = null;
        if (peakPoint != null && peakPoint.getCurrentRevenue().compareTo(BigDecimal.ZERO) > 0) {
            peakPoint.setIsPeakPeriod(true);
            peakPeriod = PeakPeriodDto.builder()
                    .label(peakPoint.getLabel())
                    .date(peakPoint.getStartDate())
                    .revenue(peakPoint.getCurrentRevenue())
                    .bookingCount(peakPoint.getBookingCount())
                    .build();
        }

        // Totals & Overall YoY Growth
        BigDecimal totalRevenue = currentBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPreviousYearRevenue = prevYearBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double overallYoYGrowthRate = calculateGrowthRate(totalRevenue, totalPreviousYearRevenue);

        // Service Revenue Breakdown (Pie Chart)
        List<BookingItem> items;
        if (branchId != null) {
            items = bookingItemRepository.findCompletedItemsByBranchIdAndDateRange(branchId, fromDate, toDate);
        } else {
            items = bookingItemRepository.findCompletedItemsBySalonIdAndDateRange(salon.getId(), fromDate, toDate);
        }

        List<ServiceRevenueBreakdownDto> serviceBreakdown = buildServiceBreakdown(items, totalRevenue);

        return RevenueAnalyticsResponse.builder()
                .period(normalizedPeriod)
                .fromDate(fromDate)
                .toDate(toDate)
                .salonId(salon.getId())
                .branchId(branchId)
                .totalRevenue(totalRevenue)
                .totalPreviousYearRevenue(totalPreviousYearRevenue)
                .overallYoYGrowthRate(overallYoYGrowthRate)
                .peakPeriod(peakPeriod)
                .timeline(timeline)
                .serviceBreakdown(serviceBreakdown)
                .build();
    }

    private List<RevenueTimePointDto> buildTimeline(
            String period,
            LocalDate from,
            LocalDate to,
            Map<LocalDate, List<Booking>> currentByDate,
            Map<LocalDate, List<Booking>> prevByDate
    ) {
        List<RevenueTimePointDto> timeline = new ArrayList<>();

        if ("monthly".equalsIgnoreCase(period)) {
            LocalDate curr = from.withDayOfMonth(1);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
            while (!curr.isAfter(to)) {
                LocalDate monthEnd = curr.plusMonths(1).minusDays(1);
                if (monthEnd.isAfter(to)) monthEnd = to;

                BigDecimal currRev = sumRevenueInRange(currentByDate, curr, monthEnd);
                BigDecimal prevRev = sumRevenueInRange(prevByDate, curr.minusYears(1), monthEnd.minusYears(1));
                long bookings = countBookingsInRange(currentByDate, curr, monthEnd);

                timeline.add(RevenueTimePointDto.builder()
                        .label("Tháng " + curr.format(fmt))
                        .startDate(curr)
                        .endDate(monthEnd)
                        .currentRevenue(currRev)
                        .previousYearRevenue(prevRev)
                        .yoyGrowthRate(calculateGrowthRate(currRev, prevRev))
                        .bookingCount(bookings)
                        .isPeakPeriod(false)
                        .build());
                curr = curr.plusMonths(1);
            }
        } else if ("weekly".equalsIgnoreCase(period)) {
            LocalDate curr = from;
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            while (!curr.isAfter(to)) {
                LocalDate weekEnd = curr.plusDays(6);
                if (weekEnd.isAfter(to)) weekEnd = to;

                int weekNum = curr.get(weekFields.weekOfWeekBasedYear());
                BigDecimal currRev = sumRevenueInRange(currentByDate, curr, weekEnd);
                BigDecimal prevRev = sumRevenueInRange(prevByDate, curr.minusYears(1), weekEnd.minusYears(1));
                long bookings = countBookingsInRange(currentByDate, curr, weekEnd);

                timeline.add(RevenueTimePointDto.builder()
                        .label("Tuần " + weekNum + " (" + curr.format(DateTimeFormatter.ofPattern("dd/MM")) + ")")
                        .startDate(curr)
                        .endDate(weekEnd)
                        .currentRevenue(currRev)
                        .previousYearRevenue(prevRev)
                        .yoyGrowthRate(calculateGrowthRate(currRev, prevRev))
                        .bookingCount(bookings)
                        .isPeakPeriod(false)
                        .build());
                curr = curr.plusDays(7);
            }
        } else if ("yearly".equalsIgnoreCase(period)) {
            int startYear = from.getYear();
            int endYear = to.getYear();
            for (int year = startYear; year <= endYear; year++) {
                LocalDate yrStart = LocalDate.of(year, 1, 1);
                LocalDate yrEnd = LocalDate.of(year, 12, 31);

                BigDecimal currRev = sumRevenueInRange(currentByDate, yrStart, yrEnd);
                BigDecimal prevRev = sumRevenueInRange(prevByDate, yrStart.minusYears(1), yrEnd.minusYears(1));
                long bookings = countBookingsInRange(currentByDate, yrStart, yrEnd);

                timeline.add(RevenueTimePointDto.builder()
                        .label("Năm " + year)
                        .startDate(yrStart)
                        .endDate(yrEnd)
                        .currentRevenue(currRev)
                        .previousYearRevenue(prevRev)
                        .yoyGrowthRate(calculateGrowthRate(currRev, prevRev))
                        .bookingCount(bookings)
                        .isPeakPeriod(false)
                        .build());
            }
        } else {
            // "daily"
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                List<Booking> dayList = currentByDate.getOrDefault(d, List.of());
                BigDecimal currRev = dayList.stream()
                        .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                        .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<Booking> prevDayList = prevByDate.getOrDefault(d.minusYears(1), List.of());
                BigDecimal prevRev = prevDayList.stream()
                        .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                        .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                timeline.add(RevenueTimePointDto.builder()
                        .label(d.format(fmt))
                        .startDate(d)
                        .endDate(d)
                        .currentRevenue(currRev)
                        .previousYearRevenue(prevRev)
                        .yoyGrowthRate(calculateGrowthRate(currRev, prevRev))
                        .bookingCount((long) dayList.size())
                        .isPeakPeriod(false)
                        .build());
            }
        }

        return timeline;
    }

    private BigDecimal sumRevenueInRange(Map<LocalDate, List<Booking>> bookingsByDate, LocalDate start, LocalDate end) {
        BigDecimal sum = BigDecimal.ZERO;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<Booking> list = bookingsByDate.getOrDefault(d, List.of());
            BigDecimal dayRev = list.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            sum = sum.add(dayRev);
        }
        return sum;
    }

    private long countBookingsInRange(Map<LocalDate, List<Booking>> bookingsByDate, LocalDate start, LocalDate end) {
        long count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            count += bookingsByDate.getOrDefault(d, List.of()).size();
        }
        return count;
    }

    private List<ServiceRevenueBreakdownDto> buildServiceBreakdown(List<BookingItem> items, BigDecimal totalRevenue) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Map<String, Map<String, Object>> map = new HashMap<>();

        for (BookingItem item : items) {
            String name = "Khác";
            String catName = "Dịch vụ chung";
            Long serviceId = null;

            if (item.getService() != null) {
                name = item.getService().getName();
                serviceId = item.getService().getId();
                if (item.getService().getCategory() != null) {
                    catName = item.getService().getCategory().getName();
                }
            } else if (item.getBundle() != null) {
                name = "[Combo] " + item.getBundle().getName();
            }

            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;

            Map<String, Object> data = map.computeIfAbsent(name, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("serviceName", k);
                m.put("categoryName", "Dịch vụ chung");
                m.put("serviceId", null);
                m.put("revenue", BigDecimal.ZERO);
                m.put("itemCount", 0L);
                return m;
            });

            data.put("categoryName", catName);
            if (serviceId != null) data.put("serviceId", serviceId);
            data.put("revenue", ((BigDecimal) data.get("revenue")).add(price));
            data.put("itemCount", ((Long) data.get("itemCount")) + 1);
        }

        List<ServiceRevenueBreakdownDto> result = new ArrayList<>();
        for (Map<String, Object> entry : map.values()) {
            BigDecimal rev = (BigDecimal) entry.get("revenue");
            Double pct = 0.0;
            if (totalRevenue != null && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                pct = rev.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
                pct = Math.round(pct * 10.0) / 10.0;
            }

            result.add(ServiceRevenueBreakdownDto.builder()
                    .serviceId((Long) entry.get("serviceId"))
                    .serviceName((String) entry.get("serviceName"))
                    .categoryName((String) entry.get("categoryName"))
                    .revenue(rev)
                    .itemCount((Long) entry.get("itemCount"))
                    .percentage(pct)
                    .build());
        }

        // Sort descending by revenue
        result.sort((a, b) -> b.getRevenue().compareTo(a.getRevenue()));

        return result;
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

    @Override
    public PeakHourHeatmapResponse getPeakHourHeatmap(Long branchId, LocalDate fromDate, LocalDate toDate) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        subscriptionService.validateAdvancedAnalytics(salon.getId());

        List<Booking> bookings;
        if (branchId != null) {
            if (fromDate != null && toDate != null) {
                bookings = bookingRepository.findByBranchIdAndBookingDateBetween(branchId, fromDate, toDate);
            } else {
                bookings = bookingRepository.findByBranchId(branchId);
            }
        } else {
            if (fromDate != null && toDate != null) {
                bookings = bookingRepository.findByBranchSalonIdAndBookingDateBetween(salon.getId(), fromDate, toDate);
            } else {
                bookings = bookingRepository.findByBranchSalonId(salon.getId());
            }
        }

        if (bookings == null) {
            bookings = List.of();
        }

        List<Booking> activeBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> b.getBookingDate() != null && b.getStartTime() != null)
                .toList();

        long totalAnalysed = activeBookings.size();

        Map<String, Long> countMap = activeBookings.stream()
                .collect(Collectors.groupingBy(
                        b -> {
                            int minSlot = (b.getStartTime().getMinute() >= 30) ? 30 : 0;
                            return b.getBookingDate().getDayOfWeek().getValue() + "-" + b.getStartTime().getHour() + "-" + minSlot;
                        },
                        Collectors.counting()
                ));

        long maxCount = countMap.values().stream().max(Long::compare).orElse(0L);

        String[] dayNames = {"", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật"};
        List<PeakHourCellDto> matrix = new ArrayList<>();

        long busiestCellCount = -1;
        String busiestDay = "Chưa có dữ liệu";
        String busiestHour = "--:--";

        for (int day = 1; day <= 7; day++) {
            for (int hour = 7; hour <= 21; hour++) {
                for (int minute : new int[]{0, 30}) {
                    String key = day + "-" + hour + "-" + minute;
                    long count = countMap.getOrDefault(key, 0L);
                    double intensity = maxCount > 0 ? Math.round(((double) count / maxCount) * 100.0) / 100.0 : 0.0;
                    String hourLabel = String.format("%02d:%02d", hour, minute);

                    String endHourLabel = minute == 0
                            ? String.format("%02d:30", hour)
                            : String.format("%02d:00", hour + 1);

                    if (count > busiestCellCount && count > 0) {
                        busiestCellCount = count;
                        busiestDay = dayNames[day];
                        busiestHour = String.format("%s - %s", hourLabel, endHourLabel);
                    }

                    matrix.add(PeakHourCellDto.builder()
                            .dayOfWeek(day)
                            .dayName(dayNames[day])
                            .hourOfDay(hour)
                            .minuteOfHour(minute)
                            .hourLabel(hourLabel)
                            .bookingCount(count)
                            .intensity(intensity)
                            .build());
                }
            }
        }

        LocalDate calculatedFrom = fromDate;
        LocalDate calculatedTo = toDate;

        if ((calculatedFrom == null || calculatedTo == null) && !activeBookings.isEmpty()) {
            calculatedFrom = activeBookings.stream()
                    .map(Booking::getBookingDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);
            calculatedTo = activeBookings.stream()
                    .map(Booking::getBookingDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);
        }

        return PeakHourHeatmapResponse.builder()
                .salonId(salon.getId())
                .branchId(branchId)
                .fromDate(calculatedFrom)
                .toDate(calculatedTo)
                .totalBookingsAnalysed(totalAnalysed)
                .maxBookingCount(maxCount)
                .busiestDay(busiestDay)
                .busiestHour(busiestHour)
                .matrix(matrix)
                .build();
    }

    @Override
    public StaffPerformanceResponse getStaffPerformanceReport(String period, LocalDate fromDate, LocalDate toDate, Long branchId) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon"));

        subscriptionService.validateAdvancedAnalytics(salon.getId());

        String branchName = "Tất cả chi nhánh";
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh ID: " + branchId));
            if (!branch.getSalon().getId().equals(salon.getId())) {
                throw new IllegalArgumentException("Chi nhánh không thuộc Salon này");
            }
            branchName = branch.getName();
        }

        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;

        String periodKey = (period != null) ? period.toLowerCase() : "this_month";
        switch (periodKey) {
            case "last_month":
                LocalDate firstOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                start = (fromDate != null) ? fromDate : firstOfLastMonth;
                end = (toDate != null) ? toDate : firstOfLastMonth.plusMonths(1).minusDays(1);
                break;
            case "last_30_days":
                start = (fromDate != null) ? fromDate : today.minusDays(30);
                end = (toDate != null) ? toDate : today;
                break;
            case "custom":
                start = (fromDate != null) ? fromDate : today.minusDays(30);
                end = (toDate != null) ? toDate : today;
                break;
            case "this_month":
            default:
                start = (fromDate != null) ? fromDate : today.withDayOfMonth(1);
                end = (toDate != null) ? toDate : today;
                break;
        }

        // Lấy danh sách nhân viên
        List<com.example.salonflow.entity.Staff> staffList;
        if (branchId != null) {
            staffList = staffRepository.findByBranchId(branchId);
        } else {
            staffList = staffRepository.findByBranchSalonId(salon.getId());
        }

        // Lấy danh sách booking trong khoảng thời gian
        List<Booking> bookings;
        if (branchId != null) {
            bookings = bookingRepository.findByBranchIdAndBookingDateBetween(branchId, start, end);
        } else {
            bookings = bookingRepository.findByBranchSalonIdAndBookingDateBetween(salon.getId(), start, end);
        }

        // Chuyển LocalDate sang Instant cho Review query
        java.time.Instant startInstant = start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        java.time.Instant endInstant = end.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        // Query rating stats từ ReviewRepository
        List<Object[]> reviewStats = reviewRepository.findStaffRatingStatsBetween(startInstant, endInstant);
        Map<Long, Double> avgRatingMap = new HashMap<>();
        Map<Long, Long> countReviewMap = new HashMap<>();
        for (Object[] row : reviewStats) {
            Long sId = (Long) row[0];
            Double avgR = (Double) row[1];
            Long cntR = (Long) row[2];
            if (sId != null) {
                avgRatingMap.put(sId, (avgR != null) ? Math.round(avgR * 100.0) / 100.0 : 0.0);
                countReviewMap.put(sId, (cntR != null) ? cntR : 0L);
            }
        }

        // Tính toán các chỉ số cho từng nhân viên
        List<StaffPerformanceMetricsDto> metricsList = new ArrayList<>();

        for (com.example.salonflow.entity.Staff staff : staffList) {
            Long staffId = staff.getId();

            // Đơn booking của nhân viên này
            List<Booking> staffBookings = bookings.stream()
                    .filter(b -> b.getAssignedStaff() != null && b.getAssignedStaff().getId().equals(staffId))
                    .collect(Collectors.toList());

            long completedCount = staffBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .count();

            BigDecimal revenue = staffBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .map(Booking::getTotalPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Double avgRating = avgRatingMap.getOrDefault(staffId, 5.0);
            Long reviewCount = countReviewMap.getOrDefault(staffId, 0L);

            // Tính toán Slot lấp đầy (Shift & Booked slots)
            long bookedSlots = staffBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.CONFIRMED)
                    .count();

            // Ước tính số ca/slot làm việc khả dụng (Mỗi ca trung bình ~16 slot 30p)
            long totalShifts = Math.max(1, (ChronoUnit.DAYS.between(start, end) + 1) * 6 / 7);
            long totalAvailableSlots = Math.max(bookedSlots, totalShifts * 16);
            double occupancyRate = Math.min(100.0, Math.round((bookedSlots * 100.0 / totalAvailableSlots) * 10.0) / 10.0);

            metricsList.add(StaffPerformanceMetricsDto.builder()
                    .staffId(staffId)
                    .staffName(staff.getName())
                    .avatarUrl(staff.getAvatarUrl())
                    .specialties(staff.getSpecialties())
                    .branchId(staff.getBranch() != null ? staff.getBranch().getId() : null)
                    .branchName(staff.getBranch() != null ? staff.getBranch().getName() : "")
                    .completedBookings(completedCount)
                    .totalRevenue(revenue)
                    .avgRating(avgRating)
                    .totalReviewsCount(reviewCount)
                    .totalWorkingShifts(totalShifts)
                    .bookedSlotsCount(bookedSlots)
                    .totalAvailableSlots(totalAvailableSlots)
                    .slotOccupancyRate(occupancyRate)
                    .build());
        }

        // Tính Rank theo từng Metric
        // 1. Revenue Rank
        metricsList.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        for (int i = 0; i < metricsList.size(); i++) {
            metricsList.get(i).setRevenueRank(i + 1);
        }

        // 2. Booking Rank
        List<StaffPerformanceMetricsDto> byBookings = new ArrayList<>(metricsList);
        byBookings.sort((a, b) -> Long.compare(b.getCompletedBookings(), a.getCompletedBookings()));
        for (int i = 0; i < byBookings.size(); i++) {
            byBookings.get(i).setBookingRank(i + 1);
        }

        // 3. Rating Rank
        List<StaffPerformanceMetricsDto> byRating = new ArrayList<>(metricsList);
        byRating.sort((a, b) -> Double.compare(b.getAvgRating(), a.getAvgRating()));
        for (int i = 0; i < byRating.size(); i++) {
            byRating.get(i).setRatingRank(i + 1);
        }

        // 4. Overall Rank (tổng rank nhỏ nhất xếp trước)
        metricsList.forEach(m -> {
            int score = m.getRevenueRank() + m.getBookingRank() + m.getRatingRank();
            m.setOverallRank(score);
        });
        metricsList.sort(Comparator.comparingInt(StaffPerformanceMetricsDto::getOverallRank));
        for (int i = 0; i < metricsList.size(); i++) {
            metricsList.get(i).setOverallRank(i + 1);
        }

        // Trích xuất Top 3 nhân viên xuất sắc nhất tháng
        List<StaffPerformanceMetricsDto> top3 = metricsList.stream()
                .limit(3)
                .collect(Collectors.toList());

        // Quét Cảnh báo Rating < 3.5 trong 30 ngày gần nhất
        java.time.Instant thirtyDaysAgo = today.minusDays(30).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        List<Object[]> recentRatingStats = reviewRepository.findStaffRatingStatsSince(thirtyDaysAgo);
        List<StaffLowRatingWarningDto> warnings = new ArrayList<>();

        for (Object[] row : recentRatingStats) {
            Long sId = (Long) row[0];
            Double avgR = (Double) row[1];
            Long cntR = (Long) row[2];

            if (sId != null && avgR != null && avgR < 3.5 && cntR != null && cntR > 0) {
                staffList.stream()
                        .filter(s -> s.getId().equals(sId))
                        .findFirst()
                        .ifPresent(st -> {
                            double roundedRating = Math.round(avgR * 100.0) / 100.0;
                            warnings.add(StaffLowRatingWarningDto.builder()
                                    .staffId(st.getId())
                                    .staffName(st.getName())
                                    .avatarUrl(st.getAvatarUrl())
                                    .branchId(st.getBranch() != null ? st.getBranch().getId() : null)
                                    .branchName(st.getBranch() != null ? st.getBranch().getName() : "")
                                    .thirtyDaysAvgRating(roundedRating)
                                    .thirtyDaysReviewCount(cntR)
                                    .warningMessage(String.format("⚠️ Nhân viên %s có điểm đánh giá 30 ngày qua thấp (%s/5.0 với %d lượt đánh giá). Cần kiểm tra chất lượng phục vụ.",
                                            st.getName(), roundedRating, cntR))
                                    .build());
                        });
            }
        }

        return StaffPerformanceResponse.builder()
                .period(periodKey)
                .fromDate(start)
                .toDate(end)
                .branchId(branchId)
                .branchName(branchName)
                .top3Performers(top3)
                .lowRatingWarnings(warnings)
                .staffPerformanceList(metricsList)
                .build();
    }
}
