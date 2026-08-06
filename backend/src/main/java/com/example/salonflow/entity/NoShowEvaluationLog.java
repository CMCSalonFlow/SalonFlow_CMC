package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Thực thể lưu trữ kết quả đánh giá độ chính xác (Accuracy) hàng tuần của mô hình AI Dự đoán No-Show.
 */
@Entity
@Table(name = "no_show_evaluation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoShowEvaluationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ngày chạy đánh giá định kỳ
    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    // Ngày bắt đầu và kết thúc của tuần được kiểm tra đánh giá
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // Tổng số lượng booking được đưa vào đánh giá
    @Column(name = "total_evaluated_bookings", nullable = false)
    private Integer totalEvaluatedBookings;

    // Confusion Matrix: True Positive (Dự đoán No-Show & Thực tế No-Show/Cancelled)
    @Column(name = "true_positives", nullable = false)
    private Integer truePositives;

    // Confusion Matrix: False Positive (Dự đoán No-Show nhưng Thực tế Completed)
    @Column(name = "false_positives", nullable = false)
    private Integer falsePositives;

    // Confusion Matrix: True Negative (Dự đoán Bình thường & Thực tế Completed)
    @Column(name = "true_negatives", nullable = false)
    private Integer trueNegatives;

    // Confusion Matrix: False Negative (Dự đoán Bình thường nhưng Thực tế No-Show/Cancelled)
    @Column(name = "false_negatives", nullable = false)
    private Integer falseNegatives;

    // Đánh giá tỷ lệ Accuracy = (TP + TN) / Total
    @Column(name = "accuracy", nullable = false)
    private Double accuracy;

    // Precision = TP / (TP + FP)
    @Column(name = "precision_score")
    private Double precisionScore;

    // Recall = TP / (TP + FN)
    @Column(name = "recall_score")
    private Double recallScore;

    // F1-Score = 2 * (Precision * Recall) / (Precision + Recall)
    @Column(name = "f1_score")
    private Double f1Score;

    @Column(name = "notes")
    private String notes;
}
