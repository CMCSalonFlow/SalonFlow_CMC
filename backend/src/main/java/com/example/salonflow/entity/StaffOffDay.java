package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "staff_off_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffOffDay extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;           

    @Column(name = "date_from", nullable = false)
    private LocalDate dateFrom;

    @Column(name = "date_to", nullable = false)
    private LocalDate dateTo;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by", length = 100)
    private String createdBy;

}