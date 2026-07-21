package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.ReviewSentiment;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Review nền cho hệ thống AI sentiment.
 *
 * Bảng này chỉ giữ dữ liệu và metadata để sau này job AI có thể
 * phân tích sentiment và cập nhật lại kết quả.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", length = 20)
    private ReviewSentiment sentiment;

    @Column(name = "sentiment_confidence", precision = 5, scale = 4)
    private BigDecimal sentimentConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewSentimentStatus sentimentStatus = ReviewSentimentStatus.PENDING;

    @Column(name = "sentiment_provider", length = 50)
    private String sentimentProvider;

    @Column(name = "sentiment_analyzed_at")
    private Instant sentimentAnalyzedAt;

    @Column(name = "sentiment_error", columnDefinition = "TEXT")
    private String sentimentError;
}
