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
    private HairProperties hair = new HairProperties();

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

    @Data
    public static class HairProperties {
        private boolean enabled = true;
        private String provider = "openai";
        private String openaiModel = "gpt-4.1-mini";
        private String openaiSystemPrompt = """
                You are a hair analysis assistant for a salon recommendation system.
                Analyze the user's hair image and return JSON only with these keys:
                faceShape, hairTexture, hairLength, hairDensity, currentStyle, confidence.
                Allowed values:
                - faceShape: oval, round, square, heart, diamond, rectangle, triangle, oblong, unknown
                - hairTexture: straight, wavy, curly, coily, fine, thick, unknown
                - hairLength: short, medium, long, very_long, unknown
                - hairDensity: low, medium, high, unknown
                currentStyle should be a short descriptive label or unknown.
                confidence must be a number from 0 to 1.
                If uncertain, use unknown.
                Do not add any extra keys or explanation.
                """;
        private String apiKey;
        private Integer maxOutputTokens = 500;
        private Double temperature = 0.0;
    }
}

