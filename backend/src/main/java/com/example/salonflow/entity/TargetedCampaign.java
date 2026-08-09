package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "targeted_campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TargetedCampaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "campaign_name", nullable = false, length = 255)
    private String campaignName;

    @Column(name = "segment_type", nullable = false, length = 50)
    private String segmentType; // 'NEW', 'RETURNING', 'VIP', 'AT_RISK'

    @Column(name = "message_title", nullable = false, length = 255)
    private String messageTitle;

    @Column(name = "message_content", columnDefinition = "TEXT", nullable = false)
    private String messageContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "recipient_count", nullable = false)
    @Builder.Default
    private Integer recipientCount = 0;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "COMPLETED";
}
