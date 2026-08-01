package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.hair.HairDifficultyLevel;
import com.example.salonflow.entity.enums.hair.HairMaintenanceLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hair_styles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HairStyle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "face_shape_tags", columnDefinition = "TEXT")
    private String faceShapeTags;

    @Column(name = "hair_texture_tags", columnDefinition = "TEXT")
    private String hairTextureTags;

    @Column(name = "hair_length_tags", columnDefinition = "TEXT")
    private String hairLengthTags;

    @Column(name = "hair_density_tags", columnDefinition = "TEXT")
    private String hairDensityTags;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private HairDifficultyLevel difficultyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_level", length = 20)
    private HairMaintenanceLevel maintenanceLevel;

    @Column(name = "price_min", precision = 12, scale = 2)
    private BigDecimal priceMin;

    @Column(name = "price_max", precision = 12, scale = 2)
    private BigDecimal priceMax;

    @Column(name = "popularity_score", precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal popularityScore = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    @OneToMany(mappedBy = "hairStyle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HairStyleImage> images = new ArrayList<>();
}
