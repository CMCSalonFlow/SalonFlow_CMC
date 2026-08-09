package com.example.salonflow.ai.dto.hair;

import com.example.salonflow.entity.enums.hair.HairDifficultyLevel;
import com.example.salonflow.entity.enums.hair.HairMaintenanceLevel;

import java.math.BigDecimal;

public record HairStyleCandidateScoreRequest(
        HairStyleAnalysisResult analysis,
        Long styleId,
        String styleCode,
        String styleName,
        String faceShapeTags,
        String hairTextureTags,
        String hairLengthTags,
        String hairDensityTags,
        HairDifficultyLevel difficultyLevel,
        HairMaintenanceLevel maintenanceLevel,
        BigDecimal popularityScore
) {
}
