package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.hair.HairAnalysisStatus;
import com.example.salonflow.entity.enums.hair.HairDensity;
import com.example.salonflow.entity.enums.hair.HairFaceShape;
import com.example.salonflow.entity.enums.hair.HairLength;
import com.example.salonflow.entity.enums.hair.HairTexture;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "hair_analysis_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HairAnalysisResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private MediaFile media;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "analysis_version", length = 50)
    private String analysisVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private HairAnalysisStatus status = HairAnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "face_shape", length = 30)
    private HairFaceShape faceShape;

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_texture", length = 30)
    private HairTexture hairTexture;

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_length", length = 30)
    private HairLength hairLength;

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_density", length = 30)
    private HairDensity hairDensity;

    @Column(name = "current_style", length = 255)
    private String currentStyle;

    @Column(name = "confidence", precision = 6, scale = 4)
    private BigDecimal confidence;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;
}
