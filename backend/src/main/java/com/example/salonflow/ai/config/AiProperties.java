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
}

