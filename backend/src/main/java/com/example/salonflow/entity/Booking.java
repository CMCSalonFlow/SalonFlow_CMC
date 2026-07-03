package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Booking — lưu trữ thông tin đặt lịch.
 *
 * Luồng:
 *   1. User chọn slot → POST /api/v1/bookings/lock (Redis SETNX)
 *   2. Nếu lock thành công → tạo Booking với status=PENDING
 *   3. User xác nhận → status=CONFIRMED
 *   4. Hủy hoặc hết TTL → status=CANCELLED, unlock Redis
 *
 * ⚠️ THAY ĐỔI so với bản gốc: thêm field recurringBooking để
 * hỗ trợ recurring booking (đặt lịch định kỳ). Nếu booking này
 * sinh ra từ 1 chuỗi lặp, recurringBooking sẽ trỏ tới
 * RecurringBooking tương ứng; nếu là booking đơn lẻ thì NULL.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /**
     * THÊM MỚI: liên kết tới RecurringBooking nếu booking này
     * là 1 phần của chuỗi lặp định kỳ. NULL nếu là booking đơn lẻ.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_booking_id")
    private RecurringBooking recurringBooking;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Key dùng trong Redis để lock slot.
     * Format: "slot:{branchId}:{staffId}:{date}:{startTime}"
     * VD: "slot:1:5:2026-06-30:09:00"
     */
    @Column(name = "slot_key", nullable = false, length = 255)
    private String slotKey;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
