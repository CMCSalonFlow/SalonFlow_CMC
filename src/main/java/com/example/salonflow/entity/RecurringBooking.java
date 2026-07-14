package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.RecurringBookingStatus;
import com.example.salonflow.entity.enums.RecurringPattern;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RecurringBooking — "công thức" lặp lại của 1 chuỗi booking.
 * Mỗi recurring booking sinh ra nhiều bản ghi Booking riêng biệt
 * (mỗi ngày lặp = 1 booking), liên kết qua Booking.recurringBooking.
 */
@Entity
@Table(name = "recurring_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringBooking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private SalonService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringPattern pattern;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private RecurringBookingStatus status = RecurringBookingStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @OneToMany(mappedBy = "recurringBooking")
    private List<Booking> bookings = new ArrayList<>();
}
