package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cancellation_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false, unique = true)
    private Salon salon;

    @Column(name = "free_cancel_hours", nullable = false)
    private Integer freeCancelHours = 24; // Mặc định 24 giờ

    @Column(name = "fee_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal feePercentage = BigDecimal.valueOf(10.00); // Mặc định 10%

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}