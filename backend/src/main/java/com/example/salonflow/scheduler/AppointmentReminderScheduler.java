package com.example.salonflow.scheduler;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.SmsReminderLog;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.notification.BookingNotificationEvent;
import com.example.salonflow.notification.BookingNotificationType;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.SmsReminderLogRepository;
import com.example.salonflow.services.service.SmsService;
import com.example.salonflow.services.service.ZaloZnsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Cron job chạy mỗi 15 phút để gửi SMS nhắc hẹn trước 24h và 1h (US-037), đồng thời bắn notification event và Zalo ZNS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final BookingRepository bookingRepository;
    private final SmsReminderLogRepository smsReminderLogRepository;
    private final SmsService smsService;
    private final ZaloZnsService zaloZnsService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Chạy mỗi 15 phút.
     * Check booking sắp tới trong khoảng 24h và 1h để gửi SMS, Zalo ZNS và Email Event nhắc hẹn.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void sendAppointmentReminders() {
        log.info("Running scheduled task for 24h appointment reminders...");
        log.info("=== [US-037] Bat dau check SMS nhac hen ===");

        LocalDateTime now = LocalDateTime.now();

        // Window 24h: từ 23h45 đến 24h15 trước lịch hẹn
        LocalDateTime window24hStart = now.plusHours(23).plusMinutes(45);
        LocalDateTime window24hEnd   = now.plusHours(24).plusMinutes(15);

        // Window 1h: từ 45 phút đến 1h15 trước lịch hẹn
        LocalDateTime window1hStart  = now.plusMinutes(45);
        LocalDateTime window1hEnd    = now.plusHours(1).plusMinutes(15);

        // Lấy tất cả booking CONFIRMED hoặc PENDING trong khoảng thời gian cần check
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                          || b.getStatus() == BookingStatus.PENDING)
                .filter(b -> b.getBookingDate() != null && b.getStartTime() != null)
                .toList();

        int sent24h = 0, sent1h = 0, skipped = 0;

        for (Booking booking : bookings) {
            if (booking.getCustomer() == null || booking.getCustomer().getId() == null) {
                continue;
            }

            LocalDateTime bookingDateTime = LocalDateTime.of(
                    booking.getBookingDate(), booking.getStartTime());

            boolean in24hWindow = bookingDateTime.isAfter(window24hStart)
                    && bookingDateTime.isBefore(window24hEnd);
            boolean in1hWindow  = bookingDateTime.isAfter(window1hStart)
                    && bookingDateTime.isBefore(window1hEnd);

            if (!in24hWindow && !in1hWindow) continue;

            if (in24hWindow) {
                // Logic 24h Notification Event & Zalo ZNS (Current/HEAD)
                String reminderKey = "email:reminder:24h:booking:" + booking.getId();
                Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(reminderKey, "1", Duration.ofDays(2));
                if (Boolean.TRUE.equals(firstTime)) {
                    try {
                        publishReminderEvent(booking);
                        if (booking.getCustomer().getPhone() != null && !booking.getCustomer().getPhone().isBlank()) {
                            zaloZnsService.sendAppointmentReminderZns(booking, booking.getCustomer());
                        }
                        log.info("Processed 24h event & ZNS reminder for booking ID={}", booking.getId());
                    } catch (Exception e) {
                        redisTemplate.delete(reminderKey);
                        log.error("Failed to process 24h event & ZNS reminder for booking ID={}: {}", booking.getId(), e.getMessage());
                    }
                }

                // Logic 24h SMS Reminder (Incoming)
                if (sendReminder(booking, "24H")) sent24h++;
                else skipped++;
            }

            if (in1hWindow) {
                // Logic 1h SMS Reminder (Incoming)
                if (sendReminder(booking, "1H")) sent1h++;
                else skipped++;
            }
        }

        log.info("=== [US-037] Ket qua SMS: gui 24h={}, gui 1h={}, bo qua={} ===",
                sent24h, sent1h, skipped);
    }

    /**
     * Gửi SMS nhắc hẹn cho 1 booking với loại nhắc (24H hoặc 1H).
     * Dedup: bỏ qua nếu đã gửi trước đó.
     */
    private boolean sendReminder(Booking booking, String reminderType) {
        Long bookingId = booking.getId();

        // Dedup check
        if (smsReminderLogRepository.existsByBookingIdAndReminderType(bookingId, reminderType)) {
            log.debug("Da gui SMS {} cho booking {}, bo qua", reminderType, bookingId);
            return false;
        }

        User customer = booking.getCustomer();
        if (customer == null || customer.getPhone() == null) {
            log.warn("Booking {} khong co thong tin khach hang hoac so dien thoai", bookingId);
            return false;
        }

        String phone = customer.getPhone();
        String message = buildSmsMessage(booking, reminderType);

        boolean success = smsService.sendSms(phone, message);

        // Lưu log dù thành công hay thất bại (để tránh retry vô hạn)
        SmsReminderLog logEntry = SmsReminderLog.builder()
                .bookingId(bookingId)
                .reminderType(reminderType)
                .phone(phone)
                .sentAt(Instant.now())
                .success(success)
                .build();
        smsReminderLogRepository.save(logEntry);

        if (success) {
            log.info("Gui SMS {} thanh cong cho booking {} - phone {}",
                    reminderType, bookingId, phone);
        } else {
            log.warn("Gui SMS {} that bai cho booking {} - phone {}",
                    reminderType, bookingId, phone);
        }

        return success;
    }

    /**
     * Tạo nội dung SMS ngắn gọn ≤160 ký tự.
     */
    private String buildSmsMessage(Booking booking, String reminderType) {
        String time = booking.getStartTime() != null
                ? booking.getStartTime().format(TIME_FMT) : "?";
        String date = booking.getBookingDate() != null
                ? booking.getBookingDate().format(DATE_FMT) : "?";
        String branch = booking.getBranch() != null
                ? booking.getBranch().getName() : "SalonFlow";
        String customerName = booking.getCustomer() != null
                && booking.getCustomer().getFullName() != null
                ? booking.getCustomer().getFullName() : "Quy khach";

        String timeLabel = "24H".equals(reminderType) ? "24 gio" : "1 gio";

        // Template SMS ≤160 ký tự
        String msg = String.format(
                "SalonFlow: %s oi, lich hen luc %s ngay %s tai %s sap den sau %s nua. Vui long den dung gio!",
                customerName, time, date, branch, timeLabel
        );

        // Đảm bảo không quá 160 ký tự
        if (msg.length() > 160) {
            msg = msg.substring(0, 160);
        }

        return msg;
    }

    private void publishReminderEvent(Booking booking) {
        applicationEventPublisher.publishEvent(
                new BookingNotificationEvent(
                        booking.getId(),
                        booking.getBranch() != null ? booking.getBranch().getId() : null,
                        booking.getCustomer() != null ? booking.getCustomer().getId() : null,
                        List.of(booking.getCustomer().getId()),
                        BookingNotificationType.APPOINTMENT_REMINDER,
                        "Nhắc lịch hẹn #" + booking.getId(),
                        "Lịch hẹn của bạn sẽ diễn ra trong 24 giờ tới.",
                        """
                        {
                          "bookingId": %d,
                          "bookingDate": "%s",
                          "startTime": "%s",
                          "branchName": "%s"
                        }
                        """.formatted(
                                booking.getId(),
                                booking.getBookingDate(),
                                booking.getStartTime(),
                                booking.getBranch() != null ? booking.getBranch().getName() : ""
                        )
                )
        );
    }
}
