package com.example.salonflow.scheduler;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cron job tự động quét các lịch hẹn quá hạn:
 * 1. Tự động HỦY các lịch hẹn PENDING / CONFIRMED quá thời gian hẹn mà khách không đến (No-Show).
 * 2. Tự động HOÀN THÀNH các lịch hẹn CHECKED_IN từ ngày cũ mà nhân viên chưa chốt đơn (tránh treo lịch).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentAutoExpiryScheduler {

    private final BookingRepository bookingRepository;

    /**
     * Chạy mỗi 10 phút, và tự động chạy sau 5 giây khi backend khởi động.
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Scheduled(initialDelay = 5000, fixedRate = 600000)
    @Transactional
    public void processExpiredAppointments() {
        log.info("=== [Auto-Expiry Task] Bat dau quet va xu ly lich hen qua han ===");
        LocalDateTime now = LocalDateTime.now();

        List<Booking> activeBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING
                          || b.getStatus() == BookingStatus.CONFIRMED
                          || b.getStatus() == BookingStatus.CHECKED_IN)
                .filter(b -> b.getBookingDate() != null)
                .toList();

        if (activeBookings.isEmpty()) {
            log.info("=== [Auto-Expiry Task] Khong co lich hen active nao can quet ===");
            return;
        }

        List<Booking> updatedBookings = new ArrayList<>();
        int autoCancelled = 0;
        int autoCompleted = 0;

        for (Booking booking : activeBookings) {
            LocalTime endTime = booking.getEndTime() != null 
                    ? booking.getEndTime() 
                    : (booking.getStartTime() != null ? booking.getStartTime().plusMinutes(30) : LocalTime.of(23, 59));

            LocalDateTime bookingEndDateTime = LocalDateTime.of(booking.getBookingDate(), endTime);

            // 1. Nếu trạng thái là PENDING hoặc CONFIRMED mà đã quá thời gian kết thúc 30 phút -> Tự động HỦY (No-Show quá hạn)
            if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
                if (now.isAfter(bookingEndDateTime.plusMinutes(30))) {
                    booking.setStatus(BookingStatus.CANCELLED);
                    String systemNotes = "[Tự động hủy bởi hệ thống]: Quá hạn lịch hẹn (Khách không đến / No-Show)";
                    if (booking.getNotes() == null || booking.getNotes().isBlank()) {
                        booking.setNotes(systemNotes);
                    } else {
                        booking.setNotes(booking.getNotes() + " | " + systemNotes);
                    }
                    updatedBookings.add(booking);
                    autoCancelled++;
                    log.info("Auto-cancelled expired booking ID={} (Date: {}, Time: {})", 
                            booking.getId(), booking.getBookingDate(), booking.getStartTime());
                }
            }
            // 2. Nếu trạng thái là CHECKED_IN mà đã quá ngày hoặc quá thời gian kết thúc 4 tiếng -> Tự động HOÀN THÀNH (tránh treo ca)
            else if (booking.getStatus() == BookingStatus.CHECKED_IN) {
                if (now.isAfter(bookingEndDateTime.plusHours(4))) {
                    booking.setStatus(BookingStatus.COMPLETED);
                    String systemNotes = "[Tự động hoàn thành bởi hệ thống]: Khách đã check-in và hết ca phục vụ";
                    if (booking.getNotes() == null || booking.getNotes().isBlank()) {
                        booking.setNotes(systemNotes);
                    } else {
                        booking.setNotes(booking.getNotes() + " | " + systemNotes);
                    }
                    updatedBookings.add(booking);
                    autoCompleted++;
                    log.info("Auto-completed checked-in booking ID={} (Date: {}, Time: {})", 
                            booking.getId(), booking.getBookingDate(), booking.getStartTime());
                }
            }
        }

        if (!updatedBookings.isEmpty()) {
            bookingRepository.saveAll(updatedBookings);
            log.info("=== [Auto-Expiry Task] Hoan tat: Tu dong Huy = {}, Tu dong Hoan thanh = {} ===", 
                    autoCancelled, autoCompleted);
        } else {
            log.info("=== [Auto-Expiry Task] Khong co lich hen qua han nao can cap nhat ===");
        }
    }
}
