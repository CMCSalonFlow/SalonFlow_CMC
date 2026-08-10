package com.example.salonflow.scheduler;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.services.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportScheduler {

    private final SalonRepository salonRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    /**
     * Tự động gửi email báo cáo tuần mới vào 8:00 AM thứ 2 hàng tuần
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void sendWeeklyReportEmailToOwners() {
        log.info("=== Bắt đầu thực thi Scheduled Job: Gửi Email Báo Cáo Tuần (8h sáng thứ 2) ===");
        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfLastWeek = today.minusDays(7);
            LocalDate endOfLastWeek = today.minusDays(1);

            List<Salon> salons = salonRepository.findAll();

            for (Salon salon : salons) {
                if (salon.getOwner() == null || salon.getOwner().getEmail() == null) {
                    continue;
                }

                List<Booking> bookings = bookingRepository.findByBranchSalonIdAndBookingDateBetween(
                        salon.getId(), startOfLastWeek, endOfLastWeek);

                long completedCount = bookings.stream()
                        .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                        .count();

                BigDecimal totalRevenue = bookings.stream()
                        .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                        .map(Booking::getTotalPrice)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Tìm Top Stylist tuần qua
                Map<String, Long> staffCountMap = bookings.stream()
                        .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getAssignedStaff() != null)
                        .collect(Collectors.groupingBy(b -> b.getAssignedStaff().getName(), Collectors.counting()));

                String topStaffName = staffCountMap.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Chưa có");

                Map<String, Object> reportData = new HashMap<>();
                reportData.put("totalRevenueStr", String.format("%,d đ", totalRevenue.longValue()));
                reportData.put("completedBookings", completedCount);
                reportData.put("topStaffName", topStaffName);

                emailService.sendWeeklyReportEmail(
                        salon.getOwner().getEmail(),
                        salon.getOwner().getFullName(),
                        salon.getName(),
                        reportData
                );

                log.info("Đã gửi email báo cáo tuần thành công cho Owner salon: {} ({})", salon.getName(), salon.getOwner().getEmail());
            }
        } catch (Exception e) {
            log.error("Lỗi khi chạy Scheduled Job gửi email báo cáo tuần: {}", e.getMessage(), e);
        }
    }
}
