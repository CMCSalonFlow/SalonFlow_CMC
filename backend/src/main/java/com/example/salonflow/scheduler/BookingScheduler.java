package com.example.salonflow.scheduler;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.salonflow.services.service.BookingService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Scheduled(cron = "0 */5 * * * *") // chạy mỗi 5 phút
    public void cancelUnpaidOnlineBookings() {
        log.info("Running scheduled task to cancel unpaid online bookings...");
        bookingService.cancelUnpaidBookings();
    }

    @Scheduled(cron = "0 */10 * * * *") // chạy mỗi 10 phút
    @Transactional
    public void cancelExpiredPendingBookings() {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Booking> bookings =
                bookingRepository.findExpiredPendingBookings(
                        BookingStatus.PENDING,
                        today,
                        now);

        for (Booking booking : bookings) {
            CancellationResult result = bookingService.cancelBooking(
                    booking.getId(),
                    "Tự động hủy do quá hạn xác nhận"
            );

            log.info("Auto cancelled booking {}", booking.getId());
        }
    }
}
