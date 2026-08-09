package com.example.salonflow.ai.provider;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.hair.HairVisionAnalysisRequest;
import com.example.salonflow.ai.dto.hair.HairVisionAnalysisResult;
import com.example.salonflow.entity.enums.hair.HairDensity;
import com.example.salonflow.entity.enums.hair.HairFaceShape;
import com.example.salonflow.entity.enums.hair.HairLength;
import com.example.salonflow.entity.enums.hair.HairTexture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiHairVisionProvider implements HairVisionProvider {

    @Qualifier("openAiWebClient")
    private final WebClient openAiWebClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public OpenAiHairVisionProvider(
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
    public HairVisionAnalysisResult analyze(HairVisionAnalysisRequest request) {
        AiProperties.HairProperties hair = aiProperties.getHair();
        if (!aiProperties.isEnabled() || hair == null || !hair.isEnabled()) {
            return fallbackUnknown("Hair AI is disabled");
        }

        if (hair.getApiKey() == null || hair.getApiKey().isBlank()) {
            return fallbackUnknown("Hair AI api key is missing");
        }

        if (request == null || request.imageDataUrl() == null || request.imageDataUrl().isBlank()) {
            return fallbackUnknown("Hair image input is empty");
        }

        try {
            Map<String, Object> payload = Map.of(
                    "model", hair.getOpenaiModel(),
                    "temperature", hair.getTemperature(),
                    "max_tokens", hair.getMaxOutputTokens(),
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", hair.getOpenaiSystemPrompt()
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "text",
                                                    "text", buildUserPrompt(request)
                                            ),
                                            Map.of(
                                                    "type", "image_url",
                                                    "image_url", Map.of(
                                                            "url", request.imageDataUrl(),
                                                            "detail", "high"
                                                    )
                                            )
                                    )
                            )
                    )
            );

            JsonNode root = openAiWebClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + hair.getApiKey())
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(60));

            String content = extractContent(root);
            if (content == null || content.isBlank()) {
                return fallbackUnknown("OpenAI returned empty content");
            }

            JsonNode resultNode = objectMapper.readTree(content);
            return new HairVisionAnalysisResult(
                    parseFaceShape(resultNode.path("faceShape").asText(null)),
                    parseHairTexture(resultNode.path("hairTexture").asText(null)),
                    parseHairLength(resultNode.path("hairLength").asText(null)),
                    parseHairDensity(resultNode.path("hairDensity").asText(null)),
                    normalizeCurrentStyle(resultNode.path("currentStyle").asText(null)),
                    parseConfidence(resultNode.path("confidence").asText(null)),
                    providerName(),
                    content
            );
        } catch (Exception ex) {
            log.warn("OpenAI hair vision analysis failed for media {}: {}", request.mediaId(), ex.getMessage());
            return fallbackUnknown(ex.getMessage());
        }
    }

    private String buildUserPrompt(HairVisionAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze this hairstyle photo.\n");
        sb.append("Return only JSON with faceShape, hairTexture, hairLength, hairDensity, currentStyle, confidence.\n");
        sb.append("Image source mediaId=").append(request.mediaId());
        if (request.originalFileName() != null) {
            sb.append(", file=").append(request.originalFileName());
        }
        if (request.mimeType() != null) {
            sb.append(", mimeType=").append(request.mimeType());
        }
        if (request.fileSize() != null) {
            sb.append(", fileSize=").append(request.fileSize());
        }
        return sb.toString();
    }

    private String extractContent(JsonNode root) {
        if (root == null) {
            return null;
        }
        String content = root.path("choices").path(0).path("message").path("content").asText(null);
        if (content != null && !content.isBlank()) {
            return content;
        }
        content = root.path("output_text").asText(null);
        if (content != null && !content.isBlank()) {
            return content;
        }
        return null;
    }

    private HairVisionAnalysisResult fallbackUnknown(String reason) {
        return new HairVisionAnalysisResult(
                HairFaceShape.UNKNOWN,
                HairTexture.UNKNOWN,
                HairLength.UNKNOWN,
                HairDensity.UNKNOWN,
                "unknown",
                BigDecimal.valueOf(0.50),
                providerName(),
                reason
        );
    }

    private HairFaceShape parseFaceShape(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "oval" -> HairFaceShape.OVAL;
            case "round" -> HairFaceShape.ROUND;
            case "square" -> HairFaceShape.SQUARE;
            case "heart" -> HairFaceShape.HEART;
            case "diamond" -> HairFaceShape.DIAMOND;
            case "rectangle" -> HairFaceShape.RECTANGLE;
            case "triangle" -> HairFaceShape.TRIANGLE;
            case "oblong" -> HairFaceShape.OBLONG;
            default -> HairFaceShape.UNKNOWN;
        };
    }

    private HairTexture parseHairTexture(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "straight" -> HairTexture.STRAIGHT;
            case "wavy" -> HairTexture.WAVY;
            case "curly" -> HairTexture.CURLY;
            case "coily" -> HairTexture.COILY;
            case "fine" -> HairTexture.FINE;
            case "thick" -> HairTexture.THICK;
            default -> HairTexture.UNKNOWN;
        };
    }

    private HairLength parseHairLength(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "short" -> HairLength.SHORT;
            case "medium" -> HairLength.MEDIUM;
            case "long" -> HairLength.LONG;
            case "very_long", "verylong", "long_hair" -> HairLength.VERY_LONG;
            default -> HairLength.UNKNOWN;
        };
    }

    private HairDensity parseHairDensity(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "low" -> HairDensity.LOW;
            case "medium" -> HairDensity.MEDIUM;
            case "high" -> HairDensity.HIGH;
            default -> HairDensity.UNKNOWN;
        };
    }

    private String normalizeCurrentStyle(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private BigDecimal parseConfidence(String value) {
        try {
            if (value == null || value.isBlank()) {
                return BigDecimal.valueOf(0.50);
            }
            BigDecimal confidence = new BigDecimal(value.trim());
            if (confidence.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (confidence.compareTo(BigDecimal.ONE) > 0) {
                return BigDecimal.ONE;
            }
            return confidence;
        } catch (Exception ex) {
            return BigDecimal.valueOf(0.50);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase().replace('-', '_').replace(' ', '_');
    }
}
