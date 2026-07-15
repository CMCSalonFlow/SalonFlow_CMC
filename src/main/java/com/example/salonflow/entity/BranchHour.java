package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(
        name = "branch_hours",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {
                        "branch_id",
                        "day_of_week"
                })
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed")
    private Boolean isClosed;
}
