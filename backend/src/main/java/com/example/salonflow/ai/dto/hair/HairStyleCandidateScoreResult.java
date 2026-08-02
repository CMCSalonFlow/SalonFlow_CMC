package com.example.salonflow.ai.dto.hair;

import java.math.BigDecimal;
import java.util.List;

public record HairStyleCandidateScoreResult(
        Long styleId,
        String styleCode,
        BigDecimal ruleScore,
        BigDecimal popularityScore,
        BigDecimal aiScore,
        BigDecimal finalScore,
        List<String> reasons
) {
}
