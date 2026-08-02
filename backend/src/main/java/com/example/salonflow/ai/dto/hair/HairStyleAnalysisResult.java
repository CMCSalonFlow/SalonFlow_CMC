package com.example.salonflow.ai.dto.hair;

import com.example.salonflow.entity.enums.hair.HairDensity;
import com.example.salonflow.entity.enums.hair.HairFaceShape;
import com.example.salonflow.entity.enums.hair.HairLength;
import com.example.salonflow.entity.enums.hair.HairTexture;

import java.math.BigDecimal;

public record HairStyleAnalysisResult(
        HairFaceShape faceShape,
        HairTexture hairTexture,
        HairLength hairLength,
        HairDensity hairDensity,
        String currentStyle,
        BigDecimal confidence,
        String provider,
        String rawResponse
) {
}
