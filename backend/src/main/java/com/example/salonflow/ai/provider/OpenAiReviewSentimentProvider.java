package com.example.salonflow.ai.provider;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.sentiment.ReviewSentimentAnalysisRequest;
import com.example.salonflow.ai.dto.sentiment.ReviewSentimentAnalysisResult;
import com.example.salonflow.ai.dto.sentiment.ReviewSentimentLabel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiReviewSentimentProvider implements ReviewSentimentProvider {

    @Qualifier("openAiWebClient")
    private final WebClient openAiWebClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public OpenAiReviewSentimentProvider(
            @Qualifier("openAiWebClient") WebClient openAiWebClient,
            AiProperties aiProperties,
            ObjectMapper objectMapper
    ) {
        this.openAiWebClient = openAiWebClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public ReviewSentimentAnalysisResult analyze(ReviewSentimentAnalysisRequest request) {
        if (!aiProperties.isEnabled() || aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            return fallbackNeutral("OpenAI is disabled or missing api key");
        }

        try {
            Map<String, Object> payload = Map.of(
                    "model", aiProperties.getReview().getOpenaiModel(),
                    "temperature", 0,
                    "max_tokens", aiProperties.getMaxOutputTokens(),
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", aiProperties.getReview().getOpenaiSystemPrompt()),
                            Map.of("role", "user", "content", request.content())
                    )
            );

            JsonNode root = openAiWebClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(30));

            String content = root != null
                    ? root.path("choices").path(0).path("message").path("content").asText(null)
                    : null;
            if (content == null || content.isBlank()) {
                return fallbackNeutral("OpenAI returned empty content");
            }

            JsonNode resultNode = objectMapper.readTree(content);
            ReviewSentimentLabel sentiment = parseLabel(resultNode.path("sentiment").asText(null));
            BigDecimal confidence = parseConfidence(resultNode.path("confidence").asText(null));

            return new ReviewSentimentAnalysisResult(
                    sentiment,
                    confidence,
                    providerName(),
                    content
            );
        } catch (Exception ex) {
            log.warn("OpenAI sentiment analysis failed for review {}: {}", request.reviewId(), ex.getMessage());
            return fallbackNeutral(ex.getMessage());
        }
    }

    private ReviewSentimentAnalysisResult fallbackNeutral(String rawResponse) {
        return new ReviewSentimentAnalysisResult(
                ReviewSentimentLabel.NEUTRAL,
                BigDecimal.valueOf(0.50),
                providerName(),
                rawResponse
        );
    }

    private ReviewSentimentLabel parseLabel(String value) {
        if (value == null) {
            return ReviewSentimentLabel.NEUTRAL;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.contains("pos")) {
            return ReviewSentimentLabel.POSITIVE;
        }
        if (normalized.contains("neg")) {
            return ReviewSentimentLabel.NEGATIVE;
        }
        return ReviewSentimentLabel.NEUTRAL;
    }

    private BigDecimal parseConfidence(String value) {
        try {
            if (value == null || value.isBlank()) {
                return BigDecimal.valueOf(0.50);
            }
            BigDecimal confidence = new BigDecimal(value);
            if (confidence.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (confidence.compareTo(BigDecimal.ONE) > 0) {
                return BigDecimal.ONE;
            }
            return confidence;
        } catch (Exception ignored) {
            return BigDecimal.valueOf(0.50);
        }
    }
}
