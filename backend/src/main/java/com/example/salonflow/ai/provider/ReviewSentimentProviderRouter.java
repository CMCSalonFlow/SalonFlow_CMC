package com.example.salonflow.ai.provider;

import com.example.salonflow.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewSentimentProviderRouter {

    private final AiProperties aiProperties;
    private final OpenAiReviewSentimentProvider openAiReviewSentimentProvider;
    private final HuggingFaceReviewSentimentProvider huggingFaceReviewSentimentProvider;

    public ReviewSentimentProvider resolveProvider() {
        String provider = normalize(aiProperties.getReview().getProvider());
        if ("huggingface".equals(provider) || "hf".equals(provider)) {
            return huggingFaceReviewSentimentProvider;
        }
        return openAiReviewSentimentProvider;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}

