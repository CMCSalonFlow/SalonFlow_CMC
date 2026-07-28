package com.example.salonflow.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private boolean enabled = false;
    private String provider = "openai";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey;
    private String defaultModel = "gpt-4.1-mini";
    private String embeddingModel = "text-embedding-3-small";
    private Integer maxOutputTokens = 800;
    private Double temperature = 0.3;
    private Integer ragTopK = 5;
    private Integer conversationTtlMinutes = 1440;
    private ReviewProperties review = new ReviewProperties();

    @Data
    public static class ReviewProperties {
        private boolean enabled = true;
        private String provider = "openai";
        private String openaiModel = "gpt-4.1-mini";
        private String openaiSystemPrompt = """
                You are a sentiment classifier for salon reviews.
                Classify the given review into exactly one of: positive, neutral, negative.
                Return JSON only with keys sentiment and confidence.
                sentiment must be lowercase.
                confidence must be a number between 0 and 1.
                """;
        private String huggingFaceModel = "cardiffnlp/twitter-roberta-base-sentiment-latest";
        private String huggingFaceApiKey;
        private Integer batchSize = 20;
        private Long scanIntervalMs = 30000L;
        private Double lowConfidenceThreshold = 0.55;
    }
}

