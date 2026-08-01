package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "hair_style_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HairStyleImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hair_style_id", nullable = false)
    private HairStyle hairStyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private MediaFile media;

    @Column(name = "is_cover", nullable = false)
    @Builder.Default
    private Boolean isCover = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "image_quality_score", precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal imageQualityScore = BigDecimal.ZERO;

    @Column(name = "ai_aesthetic_score", precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal aiAestheticScore = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
