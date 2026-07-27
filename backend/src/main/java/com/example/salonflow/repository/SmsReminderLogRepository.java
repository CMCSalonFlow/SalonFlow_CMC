package com.example.salonflow.repository;

import com.example.salonflow.entity.SmsReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsReminderLogRepository extends JpaRepository<SmsReminderLog, Long> {

    /**
     * Kiểm tra đã gửi SMS nhắc hẹn cho booking này với loại nhắc này chưa (dedup).
     */
    boolean existsByBookingIdAndReminderType(Long bookingId, String reminderType);
}
