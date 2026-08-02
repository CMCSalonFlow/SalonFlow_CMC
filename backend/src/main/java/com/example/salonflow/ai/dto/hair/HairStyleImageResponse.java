package com.example.salonflow.ai.dto.hair;

import java.math.BigDecimal;

public record HairStyleImageResponse(
        Long id,
        String url,
        Boolean isCover,
        Integer displayOrder,
        BigDecimal imageQualityScore,
        BigDecimal aiAestheticScore
) {
}
