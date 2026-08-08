package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salon_approval_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonApprovalAudit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "action", nullable = false)
    private String action; // APPROVE, REJECT, APPEAL

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}
