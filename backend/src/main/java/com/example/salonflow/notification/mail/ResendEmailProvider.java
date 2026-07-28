package com.example.salonflow.notification.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResendEmailProvider implements EmailProvider {

    private final WebClient.Builder webClientBuilder;

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from:}")
    private String from;

    @Value("${resend.base-url:https://api.resend.com}")
    private String baseUrl;

    @Override
    public String getName() {
        return "resend";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && from != null && !from.isBlank();
    }

    @Override
    public void send(String to, String subject, String html) {
        if (!isConfigured()) {
            throw new IllegalStateException("Resend provider is not configured");
        }

        Map<String, Object> body = Map.of(
                "from", from,
                "to", to,
                "subject", subject,
                "html", html
        );

        webClientBuilder.baseUrl(baseUrl)
                .build()
                .post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Sent email via Resend to {}", to);
    }
}
