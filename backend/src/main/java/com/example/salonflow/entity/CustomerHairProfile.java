package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.hair.HairDensity;
import com.example.salonflow.entity.enums.hair.HairFaceShape;
import com.example.salonflow.entity.enums.hair.HairLength;
import com.example.salonflow.entity.enums.hair.HairTexture;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_hair_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerHairProfile extends BaseEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_hair_style_id")
    private HairStyle selectedHairStyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_hair_style_image_id")
    private HairStyleImage selectedHairStyleImage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_analysis_result_id")
    private HairAnalysisResult latestAnalysisResult;

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

    @Column(name = "profile_synced_at")
    private java.time.Instant profileSyncedAt;
}
