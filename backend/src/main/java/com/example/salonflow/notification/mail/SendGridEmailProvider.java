package com.example.salonflow.notification.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailProvider implements EmailProvider {

    private final WebClient.Builder webClientBuilder;

    @Value("${sendgrid.api-key:}")
    private String apiKey;

    @Value("${sendgrid.from:}")
    private String from;

    @Value("${sendgrid.base-url:https://api.sendgrid.com/v3}")
    private String baseUrl;

    @Override
    public String getName() {
        return "sendgrid";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && from != null && !from.isBlank();
    }

    @Override
    public void send(String to, String subject, String html) {
        if (!isConfigured()) {
            throw new IllegalStateException("SendGrid provider is not configured");
        }

        Map<String, Object> body = Map.of(
                "personalizations", List.of(
                        Map.of("to", List.of(Map.of("email", to)))
                ),
                "from", Map.of("email", from),
                "subject", subject,
                "content", List.of(
                        Map.of("type", "text/html", "value", html)
                )
        );

        webClientBuilder.baseUrl(baseUrl)
                .build()
                .post()
                .uri("/mail/send")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();

        log.info("Sent email via SendGrid to {}", to);
    }
}
