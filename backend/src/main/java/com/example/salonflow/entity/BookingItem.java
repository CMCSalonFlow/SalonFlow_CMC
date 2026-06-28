package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Thực thể lưu trữ chi tiết từng dịch vụ hoặc combo được đặt trong một lịch hẹn (Booking).
 */
@Entity
@Table(name = "booking_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với thực thể lịch hẹn chính
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Dịch vụ lẻ được đặt (có thể null nếu đặt theo combo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    // Combo/Gói dịch vụ được đặt (có thể null nếu đặt dịch vụ lẻ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id")
    private ServiceBundle bundle;

    // Giá của dịch vụ/combo tại thời điểm đặt lịch
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // Thời lượng thực hiện (tính bằng phút) tại thời điểm đặt lịch
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
}
