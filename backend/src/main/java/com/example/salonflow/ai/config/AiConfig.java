package com.example.salonflow.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    public WebClient openAiWebClient(
            WebClient.Builder builder,
            AiProperties properties
    ) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Bean
    public WebClient huggingFaceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api-inference.huggingface.co")
                .build();
    }
}

