package com.example.salonflow.ai.dto.hair;

import com.example.salonflow.entity.enums.hair.HairDifficultyLevel;
import com.example.salonflow.entity.enums.hair.HairMaintenanceLevel;

import java.math.BigDecimal;
import java.util.List;

public record HairStyleRecommendationItem(
        Long styleId,
        String styleCode,
        String styleName,
        String description,
        HairStyleImageResponse sampleImage,
        HairDifficultyLevel difficultyLevel,
        HairMaintenanceLevel maintenanceLevel,
        String priceRange,
        BigDecimal ruleScore,
        BigDecimal aiScore,
        BigDecimal finalScore,
        List<String> reasons
) {
}
