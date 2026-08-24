package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ kết quả dự đoán No-Show của AI cho từng lịch hẹn đặt chỗ (Booking).
 */
@Entity
@Table(name = "no_show_prediction_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoShowPredictionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    // Xác suất no-show tính bằng Logistic Regression: P(no-show) trong khoảng [0.0, 1.0]
    @Column(name = "probability", nullable = false)
    private Double probability;

    // Mức độ rủi ro: LOW, MEDIUM, HIGH (HIGH khi probability > 0.7)
    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    // JSON chứa chi tiết 4 đặc trưng đã được trích xuất (cancelRate, distanceKm, leadTimeHours, completedCount)
    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    // Chuỗi mô tả lý do dự đoán (vd: Lịch sử hủy cao 40%, Đặt lịch trước 14 ngày, v.v.)
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    // Đánh dấu đã kích hoạt cảnh báo nguy cơ cao cho staff
    @Column(name = "is_warning_triggered")
    @Builder.Default
    private Boolean isWarningTriggered = false;

    // Đánh dấu đã tự động gửi Email/SMS nhắc nhở cho khách
    @Column(name = "sms_sent")
    @Builder.Default
    private Boolean smsSent = false;

    @Column(name = "sms_sent_at")
    private LocalDateTime smsSentAt;
}
