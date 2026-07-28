package com.example.salonflow.ai.dto.sentiment;

public record ReviewSentimentAnalysisRequest(
        Long reviewId,
        String content
) {
}

