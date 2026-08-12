package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể đại diện cho lịch đặt dịch vụ (Booking) của khách hàng tại chi nhánh.
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

    // Khách hàng đặt lịch hẹn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // Chi nhánh diễn ra dịch vụ đặt lịch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Ngày đặt lịch hẹn
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    // Giờ bắt đầu thực hiện lịch hẹn
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // Giờ kết thúc thực hiện lịch hẹn
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Nhân sự được khách hàng yêu thích chọn lựa (có thể để trống)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_staff_id")
    private Staff preferredStaff;

    // Nhân sự thực tế được phân bổ để làm dịch vụ cho khách hàng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private Staff assignedStaff;

    // Trạng thái của lịch hẹn
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    // Tổng số tiền của các dịch vụ trong hóa đơn đặt lịch
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    // Số tiền cần đặt cọc cho booking này
    @Column(name = "deposit_amount", precision = 12, scale = 2)
    private BigDecimal depositAmount;

    // Tổng thời gian dự kiến thực hiện (tính bằng phút)
    @Column(name = "total_duration_minutes", nullable = false)
    private Integer totalDurationMinutes;

    // Các lưu ý, ghi chú từ phía khách hàng
    @Column(columnDefinition = "TEXT")
    private String notes;

    // URL hóa đơn PDF trên MinIO
    @Column(name = "invoice_url")
    private String invoiceUrl;

    @Column(name = "remaining_amount", precision = 12, scale = 2)
    private BigDecimal remainingAmount;
    // Chi tiết danh sách dịch vụ lẻ hoặc combo tương ứng với lượt đặt lịch này
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingItem> items = new ArrayList<>();

    /**
     * THÊM MỚI: liên kết tới RecurringBooking nếu booking này
     * là 1 phần của chuỗi lặp định kỳ. NULL nếu là booking đơn lẻ.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_booking_id")
    private RecurringBooking recurringBooking;

    /**
     * Key dùng trong Redis để lock slot.
     * Format: "slot:{branchId}:{staffId}:{date}:{startTime}"
     * VD: "slot:1:5:2026-06-30:09:00"
     */
    @Column(name = "slot_key", length = 255)
    private String slotKey;

    @Column(name = "invoice_generated_at")
    private LocalDateTime invoiceGeneratedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

}
