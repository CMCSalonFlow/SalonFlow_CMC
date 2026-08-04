package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.description.ServiceDescriptionGenerateRequest;
import com.example.salonflow.ai.dto.description.ServiceDescriptionGenerateResponse;
import com.example.salonflow.ai.dto.description.ServiceDescriptionQuotaResponse;
import com.example.salonflow.ai.prompt.ServiceDescriptionPromptBuilder;
import com.example.salonflow.ai.service.ServiceDescriptionGenerationService;
import com.example.salonflow.ai.service.ServiceDescriptionQuotaService;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.SalonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ServiceDescriptionGenerationServiceImpl implements ServiceDescriptionGenerationService {

    private final SalonRepository salonRepository;
    private final ServiceDescriptionQuotaService quotaService;
    private final ServiceDescriptionPromptBuilder promptBuilder;
    private final AiProperties aiProperties;
    private final WebClient openAiWebClient;

    public ServiceDescriptionGenerationServiceImpl(
            SalonRepository salonRepository,
            ServiceDescriptionQuotaService quotaService,
            ServiceDescriptionPromptBuilder promptBuilder,
            AiProperties aiProperties,
            @Qualifier("openAiWebClient") WebClient openAiWebClient
    ) {
        this.salonRepository = salonRepository;
        this.quotaService = quotaService;
        this.promptBuilder = promptBuilder;
        this.aiProperties = aiProperties;
        this.openAiWebClient = openAiWebClient;
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceDescriptionGenerateResponse generate(Long ownerId, ServiceDescriptionGenerateRequest request) {
        validateRequest(ownerId, request);

        Salon salon = resolveOwnedSalon(ownerId, request.salonId());
        AiProperties.ServiceDescriptionProperties props = aiProperties.getServiceDescription();
        ensureEnabled(props);
        ensureApiKeyAvailable(props);

        ServiceDescriptionQuotaResponse quota = quotaService.consumeQuota(salon.getId());
        String prompt = promptBuilder.buildPrompt(request);
        String description = callOpenAi(props, prompt);
        description = normalizeDescription(description);

        if (description.isBlank()) {
            throw new BadRequestException("AI returned empty service description");
        }

        return new ServiceDescriptionGenerateResponse(
                salon.getId(),
                request.serviceName().trim(),
                sanitizeKeywords(request.keywords()),
                description,
                props.getProvider(),
                props.getOpenaiModel(),
                1,
                quota.usedToday(),
                quota.dailyLimit(),
                quota.remainingToday(),
                Instant.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceDescriptionQuotaResponse getQuota(Long ownerId, Long salonId) {
        resolveOwnedSalon(ownerId, salonId);
        return quotaService.getQuota(salonId);
    }

    private void validateRequest(Long ownerId, ServiceDescriptionGenerateRequest request) {
        if (ownerId == null) {
            throw new BadRequestException("Owner id is required");
        }
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.salonId() == null) {
            throw new BadRequestException("Salon id is required");
        }
        if (request.serviceName() == null || request.serviceName().isBlank()) {
            throw new BadRequestException("Service name is required");
        }
        if (request.keywords() == null || request.keywords().size() < 3 || request.keywords().size() > 5) {
            throw new BadRequestException("Keywords must contain between 3 and 5 items");
        }
    }

    private Salon resolveOwnedSalon(Long ownerId, Long salonId) {
        return salonRepository.findByIdAndOwnerId(salonId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found for this owner"));
    }

    private void ensureEnabled(AiProperties.ServiceDescriptionProperties props) {
        if (!aiProperties.isEnabled() || props == null || !props.isEnabled()) {
            throw new BadRequestException("Service description AI is disabled");
        }
    }

    private void ensureApiKeyAvailable(AiProperties.ServiceDescriptionProperties props) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BadRequestException("OpenAI API key is missing");
        }
    }

    private String callOpenAi(AiProperties.ServiceDescriptionProperties props, String userPrompt) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", props.getOpenaiModel());
            payload.put("temperature", props.getTemperature());
            payload.put("max_tokens", props.getMaxOutputTokens());
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", props.getOpenaiSystemPrompt()),
                    Map.of("role", "user", "content", userPrompt)
            ));

            JsonNode root = openAiWebClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(60));

            String content = extractContent(root);
            if (content == null || content.isBlank()) {
                throw new BadRequestException("OpenAI returned empty content");
            }
            return content;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Service description generation failed: {}", ex.getMessage());
            throw new BadRequestException("Failed to generate service description: " + ex.getMessage());
        }
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

    private List<String> sanitizeKeywords(List<String> keywords) {
        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .toList();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        String normalized = description
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Integer minWords = aiProperties.getServiceDescription() != null
                ? aiProperties.getServiceDescription().getMinWords()
                : null;
        Integer maxWords = aiProperties.getServiceDescription() != null
                ? aiProperties.getServiceDescription().getMaxWords()
                : null;

        int wordCount = countWords(normalized);
        if (minWords != null && wordCount < minWords) {
            log.warn("Generated description is shorter than expected: {} words", wordCount);
        }
        if (maxWords != null && wordCount > maxWords) {
            log.warn("Generated description is longer than expected: {} words", wordCount);
        }
        return normalized;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String[] parts = text.trim().split("\\s+");
        return parts.length;
    }
}
