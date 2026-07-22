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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HuggingFaceReviewSentimentProvider implements ReviewSentimentProvider {

    @Qualifier("huggingFaceWebClient")
    private final WebClient huggingFaceWebClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public HuggingFaceReviewSentimentProvider(
            @Qualifier("huggingFaceWebClient") WebClient huggingFaceWebClient,
            AiProperties aiProperties,
            ObjectMapper objectMapper
    ) {
        this.huggingFaceWebClient = huggingFaceWebClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return "huggingface";
    }

    @Override
    public ReviewSentimentAnalysisResult analyze(ReviewSentimentAnalysisRequest request) {
        if (!aiProperties.isEnabled()) {
            return fallbackNeutral("HuggingFace is disabled");
        }

        try {
            Map<String, Object> payload = Map.of(
                    "inputs", request.content(),
                    "options", Map.of("wait_for_model", true)
            );

            WebClient.RequestHeadersSpec<?> spec = huggingFaceWebClient.post()
                    .uri("/models/" + aiProperties.getReview().getHuggingFaceModel())
                    .bodyValue(payload);

            if (hasApiKey()) {
                spec = spec.header("Authorization", buildAuthorization());
            }

            JsonNode root = spec
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(30));

            if (root == null || !root.isArray() || root.isEmpty()) {
                return fallbackNeutral("HuggingFace returned empty response");
            }

            JsonNode best = findBestScoreNode(root);
            ReviewSentimentLabel sentiment = parseLabel(best.path("label").asText(null));
            BigDecimal confidence = best.hasNonNull("score")
                    ? BigDecimal.valueOf(best.path("score").asDouble())
                    : BigDecimal.valueOf(0.50);

            return new ReviewSentimentAnalysisResult(
                    sentiment,
                    confidence,
                    providerName(),
                    root.toString()
            );
        } catch (Exception ex) {
            log.warn("HuggingFace sentiment analysis failed for review {}: {}", request.reviewId(), ex.getMessage());
            return fallbackNeutral(ex.getMessage());
        }
    }

    private boolean hasApiKey() {
        String apiKey = aiProperties.getReview().getHuggingFaceApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    private String buildAuthorization() {
        return "Bearer " + aiProperties.getReview().getHuggingFaceApiKey();
    }

    private JsonNode findBestScoreNode(JsonNode root) {
        if (root.isArray() && root.size() > 0 && root.get(0).isArray()) {
            return root.get(0).elements().next();
        }
        return root.elements()
                .next();
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

    private ReviewSentimentAnalysisResult fallbackNeutral(String rawResponse) {
        return new ReviewSentimentAnalysisResult(
                ReviewSentimentLabel.NEUTRAL,
                BigDecimal.valueOf(0.50),
                providerName(),
                rawResponse
        );
    }
}
