package com.example.salonflow.scheduler;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.services.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;

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

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setNotes("Tự động hủy do quá hạn xác nhận");

            bookingRepository.save(booking);

            CancellationResult result =
                    CancellationResult.builder()
                            .success(true)
                            .isFreeCancel(true)
                            .feeAmount(BigDecimal.ZERO)
                            .message("Booking tự động hủy do quá hạn xác nhận")
                            .build();

            emailService.sendCancellationEmail(booking, result);

            log.info("Auto cancelled booking {}", booking.getId());
        }
    }
}