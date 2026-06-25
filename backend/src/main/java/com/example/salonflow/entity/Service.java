package com.example.salonflow.entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dịch vụ cụ thể của 1 salon: tên, giá, thời gian, mô tả, ảnh.
 *
 * ⚠️ LƯU Ý: duration_minutes ảnh hưởng trực tiếp đến slot booking
 * (xem AC). Validate bội số 15 phút ở 2 lớp:
 *   1. DB constraint (chk_services_duration_multiple_of_15) — chốt cuối
 *   2. Bean Validation @DurationMultipleOf15 trên DTO — phản hồi sớm cho FE
 *
 * Bảng: services
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * Đơn vị: phút. BẮT BUỘC là bội số của 15
     * (15, 30, 45, 60...) vì đây là đơn vị chia slot booking.
     */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceImage> images = new ArrayList<>();
}