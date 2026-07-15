package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Chi tiết 1 ca trong template: thứ mấy, mấy giờ.
 * dayOfWeek: 1=Thứ 2, 2=Thứ 3, ..., 6=Thứ 7, 7=Chủ nhật (ISO 8601)
 */
@Entity
@Table(name = "shift_template_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftTemplateDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ShiftTemplate template;

    /**
     * 1=Thứ 2, 2=Thứ 3, 3=Thứ 4, 4=Thứ 5,
     * 5=Thứ 6, 6=Thứ 7, 7=Chủ nhật
     */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
