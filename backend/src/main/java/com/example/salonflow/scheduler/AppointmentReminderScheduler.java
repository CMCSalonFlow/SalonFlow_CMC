package com.example.salonflow.scheduler;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.notification.BookingNotificationEvent;
import com.example.salonflow.notification.BookingNotificationType;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.services.service.ZaloZnsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final BookingRepository bookingRepository;
    private final ZaloZnsService zaloZnsService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StringRedisTemplate redisTemplate;

    /**
     * Chạy mỗi 15 phút và gửi nhắc lịch cho booking sẽ diễn ra trong 24 giờ tới.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional(readOnly = true)
    public void sendAppointmentReminders() {
        log.info("Running scheduled task for 24h appointment reminders...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusHours(24);
        LocalDateTime to = from.plusMinutes(15);

        List<Booking> upcomingBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> b.getBookingDate() != null && b.getStartTime() != null)
                .filter(b -> {
                    LocalDateTime bookingTime = b.getBookingDate().atTime(b.getStartTime());
                    return !bookingTime.isBefore(from) && bookingTime.isBefore(to);
                })
                .toList();

        for (Booking booking : upcomingBookings) {
            if (booking.getCustomer() == null || booking.getCustomer().getId() == null) {
                continue;
            }

            String reminderKey = "email:reminder:24h:booking:" + booking.getId();
            Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(reminderKey, "1", Duration.ofDays(2));
            if (Boolean.FALSE.equals(firstTime)) {
                continue;
            }

            try {
                publishReminderEvent(booking);
                if (booking.getCustomer().getPhone() != null && !booking.getCustomer().getPhone().isBlank()) {
                    zaloZnsService.sendAppointmentReminderZns(booking, booking.getCustomer());
                }
                log.info("Processed 24h reminder for booking ID={}", booking.getId());
            } catch (Exception e) {
                redisTemplate.delete(reminderKey);
                log.error("Failed to process appointment reminder for booking ID={}: {}", booking.getId(), e.getMessage());
            }
        }
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
