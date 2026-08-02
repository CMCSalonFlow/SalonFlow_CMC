package com.example.salonflow.ai.dto.hair;

import java.time.Instant;
import java.util.List;

public record HairStyleRecommendationResponse(
        Long analysisResultId,
        HairStyleAnalysisResult analysis,
        List<HairStyleRecommendationItem> suggestedStyles,
        String provider,
        Instant analyzedAt
) {
}
