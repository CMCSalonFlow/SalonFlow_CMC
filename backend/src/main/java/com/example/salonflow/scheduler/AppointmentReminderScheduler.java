package com.example.salonflow.scheduler;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.services.service.ZaloZnsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final BookingRepository bookingRepository;
    private final ZaloZnsService zaloZnsService;

    /**
     * Runs every 15 minutes to send ZNS appointment reminders for bookings in the next 2 hours.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional(readOnly = true)
    public void sendAppointmentReminders() {
        log.info("Running scheduled task to send Zalo ZNS appointment reminders...");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime targetWindow = now.plusHours(2);

        // Find upcoming confirmed bookings for today within next 2 hours
        List<Booking> upcomingBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                .filter(b -> today.equals(b.getBookingDate()))
                .filter(b -> b.getStartTime() != null && b.getStartTime().isAfter(now) && b.getStartTime().isBefore(targetWindow))
                .toList();

        for (Booking booking : upcomingBookings) {
            try {
                if (booking.getCustomer() != null) {
                    zaloZnsService.sendAppointmentReminderZns(booking, booking.getCustomer());
                    log.info("Successfully processed appointment reminder for booking ID={}", booking.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send appointment reminder for booking ID={}: {}", booking.getId(), e.getMessage());
            }
        }
    }
}
