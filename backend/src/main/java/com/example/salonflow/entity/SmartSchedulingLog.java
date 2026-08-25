package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "smart_schedule_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSchedulingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "service_ids")
    private String serviceIds;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "recommended_slots_json", columnDefinition = "TEXT")
    private String recommendedSlotsJson;

    @Column(name = "selected_slot_time")
    private String selectedSlotTime;

    @Column(name = "is_booked")
    @Builder.Default
    private Boolean isBooked = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
