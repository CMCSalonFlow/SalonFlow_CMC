package com.example.salonflow.ai.provider;

import com.example.salonflow.ai.dto.sentiment.ReviewSentimentAnalysisRequest;
import com.example.salonflow.ai.dto.sentiment.ReviewSentimentAnalysisResult;

public interface ReviewSentimentProvider {

    String providerName();

    ReviewSentimentAnalysisResult analyze(ReviewSentimentAnalysisRequest request);
}

