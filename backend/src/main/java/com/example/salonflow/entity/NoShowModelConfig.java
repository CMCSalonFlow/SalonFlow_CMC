package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Thực thể lưu trữ tham số cấu hình mô hình Logistic Regression cho AI Dự đoán No-Show.
 * Công thức: z = beta0 + beta1 * cancelRate + beta2 * distanceKmNorm + beta3 * leadTimeNorm - beta4 * completedCountNorm
 */
@Entity
@Table(name = "no_show_model_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoShowModelConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Direct Intercept beta0
    @Column(name = "beta_0", nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal beta0 = new BigDecimal("-1.500");

    // Beta 1: Lịch sử hủy/no-show (Cancel Rate)
    @Column(name = "beta_1", nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal beta1 = new BigDecimal("2.500");

    // Beta 2: Khoảng cách địa lý (Distance Norm)
    @Column(name = "beta_2", nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal beta2 = new BigDecimal("1.200");

    // Beta 3: Lead time đặt trước (Lead Time Norm)
    @Column(name = "beta_3", nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal beta3 = new BigDecimal("1.000");

    // Beta 4: Số lần đã đến dịch vụ thành công (Completed Count Norm)
    @Column(name = "beta_4", nullable = false, precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal beta4 = new BigDecimal("2.000");

    // Ngưỡng kích hoạt cảnh báo nguy cơ cao (Mặc định 0.700 = 70%)
    @Column(name = "risk_threshold", nullable = false, precision = 4, scale = 3)
    @Builder.Default
    private BigDecimal riskThreshold = new BigDecimal("0.700");

    // Tự động kích hoạt SMS/Zalo ZNS khi risk_level = HIGH
    @Column(name = "auto_send_reminder")
    @Builder.Default
    private Boolean autoSendReminder = true;

    @Column(name = "description")
    private String description;
}
