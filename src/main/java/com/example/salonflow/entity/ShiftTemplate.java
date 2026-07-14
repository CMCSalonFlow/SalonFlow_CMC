package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Template lịch làm việc tái sử dụng hàng tuần.
 * Mỗi template thuộc về 1 staff (user) trong 1 branch.
 */
@Entity
@Table(name = "shift_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @OneToMany(
            mappedBy = "template",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ShiftTemplateDetail> details = new ArrayList<>();
}
