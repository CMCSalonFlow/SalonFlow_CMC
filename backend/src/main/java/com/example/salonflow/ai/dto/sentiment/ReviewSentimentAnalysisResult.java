package com.example.salonflow.ai.dto.sentiment;

import java.math.BigDecimal;

public record ReviewSentimentAnalysisResult(
        ReviewSentimentLabel sentiment,
        BigDecimal confidence,
        String provider,
        String rawResponse
) {
}

