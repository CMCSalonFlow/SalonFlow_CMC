package com.example.salonflow.notification.fcm;

import com.example.salonflow.config.properties.FcmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushClientImpl implements FcmPushClient {

    private final FcmProperties properties;
    private final FcmAccessTokenService accessTokenService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FcmDeliveryResult sendToToken(String deviceToken, String title, String body, Map<String, String> data) {
        if (!properties.isEnabled()) {
            return FcmDeliveryResult.builder()
                    .success(false)
                    .invalidToken(false)
                    .statusCode(0)
                    .responseBody("FCM is disabled")
                    .build();
        }

        if (properties.getProjectId() == null || properties.getProjectId().isBlank()) {
            throw new IllegalStateException("Thiếu app.notification.fcm.project-id");
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", deviceToken);
        message.put("notification", Map.of(
                "title", title,
                "body", body
        ));
        if (data != null && !data.isEmpty()) {
            message.put("data", data);
        }

        String webLink = properties.getWebLink();
        if (webLink != null && !webLink.isBlank()) {
            message.put("webpush", Map.of(
                    "fcm_options", Map.of("link", webLink)
            ));
        }

        Map<String, Object> requestBody = Map.of("message", message);
        String url = "https://fcm.googleapis.com/v1/projects/%s/messages:send".formatted(properties.getProjectId());
        String accessToken = accessTokenService.getAccessToken();

        WebClient webClient = webClientBuilder.build();
        try {
            Map response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return FcmDeliveryResult.builder()
                    .success(true)
                    .invalidToken(false)
                    .statusCode(200)
                    .responseBody(response != null ? response.toString() : "{}")
                    .build();
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            boolean invalidToken = isInvalidTokenError(e.getStatusCode().value(), responseBody);
            return FcmDeliveryResult.builder()
                    .success(false)
                    .invalidToken(invalidToken)
                    .statusCode(e.getStatusCode().value())
                    .responseBody(responseBody)
                    .build();
        }
    }

    private boolean isInvalidTokenError(int statusCode, String responseBody) {
        if (statusCode == 404) {
            return true;
        }
        if (responseBody == null) {
            return false;
        }
        String normalized = responseBody.toUpperCase();
        return normalized.contains("UNREGISTERED")
                || normalized.contains("INVALID_REGISTRATION_TOKEN")
                || normalized.contains("INVALID_ARGUMENT")
                || normalized.contains("NOT FOUND");
    }
}
