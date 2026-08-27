package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Cấu hình giá tiền, giới hạn quy mô và tính năng của các Gói dịch vụ.
 */
@Entity
@Table(name = "subscription_plan_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private SubscriptionPlan plan;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    @Column(name = "yearly_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal yearlyPrice = BigDecimal.ZERO;

    @Column(name = "max_branches", nullable = false)
    @Builder.Default
    private Integer maxBranches = 1;

    @Column(name = "max_staff_per_branch", nullable = false)
    @Builder.Default
    private Integer maxStaffPerBranch = 3;

    @Column(name = "has_analytics", nullable = false)
    @Builder.Default
    private Boolean hasAnalytics = false;

    @Column(name = "has_ai", nullable = false)
    @Builder.Default
    private Boolean hasAi = false;

    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    @Column(name = "badge_text", length = 50)
    private String badgeText;

    @Column(name = "is_popular", nullable = false)
    @Builder.Default
    private Boolean isPopular = false;
}
