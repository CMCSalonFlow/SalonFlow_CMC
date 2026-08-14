package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.ReviewSentiment;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    @JoinColumn(name = "customer_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

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

    @Column(name = "content", columnDefinition = "TEXT")
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

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewPhoto> photos = new ArrayList<>();

    @Column(name = "owner_reply", columnDefinition = "TEXT")
    private String ownerReply;

    @Column(name = "owner_replied_at")
    private Instant ownerRepliedAt;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Column(name = "hidden_reason", columnDefinition = "TEXT")
    private String hiddenReason;

    // Alias methods for backwards compatibility with customer and comment field names
    public User getCustomer() {
        return user;
    }

    public void setCustomer(User customer) {
        this.user = customer;
    }

    public String getComment() {
        return content;
    }

    public void setComment(String comment) {
        this.content = comment;
    }
}
