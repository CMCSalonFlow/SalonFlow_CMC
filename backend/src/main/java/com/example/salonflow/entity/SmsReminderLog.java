package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Lưu lịch sử gửi SMS nhắc hẹn để tránh gửi 2 lần (dedup) - US-037.
 */
@Entity
@Table(
    name = "sms_reminder_logs",
    uniqueConstraints = {
        // Mỗi booking + loại nhắc (24h hoặc 1h) chỉ gửi 1 lần
        @UniqueConstraint(columnNames = {"booking_id", "reminder_type"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Booking được nhắc hẹn
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    // Loại nhắc: "24H" hoặc "1H"
    @Column(name = "reminder_type", nullable = false, length = 10)
    private String reminderType;

    // Số điện thoại đã gửi
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    // Thời điểm gửi
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    // Gửi thành công hay không
    @Column(name = "success", nullable = false)
    private boolean success;
}
