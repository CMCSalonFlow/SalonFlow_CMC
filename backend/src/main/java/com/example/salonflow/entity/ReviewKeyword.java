package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_keywords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    /** Định dạng "YYYY-MM" */
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "frequency", nullable = false)
    @Builder.Default
    private Integer frequency = 0;
}
