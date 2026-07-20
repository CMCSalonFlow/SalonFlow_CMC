package com.example.salonflow.notification.fcm;

import com.example.salonflow.config.properties.FcmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmAccessTokenServiceImpl implements FcmAccessTokenService {

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final FcmProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile CachedToken cachedToken;

    @Override
    public synchronized String getAccessToken() {
        if (cachedToken != null && cachedToken.expiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return cachedToken.token;
        }

        FirebaseServiceAccount account = loadServiceAccount();
        String jwtAssertion = createJwtAssertion(account);

        WebClient webClient = webClientBuilder.build();
        try {
            Map<String, Object> response = webClient.post()
                    .uri(account.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                            .with("assertion", jwtAssertion))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("access_token") == null) {
                throw new IllegalStateException("Không lấy được access token từ Google OAuth");
            }

            String accessToken = response.get("access_token").toString();
            int expiresIn = Integer.parseInt(response.getOrDefault("expires_in", 3600).toString());
            cachedToken = new CachedToken(accessToken, Instant.now().plusSeconds(expiresIn));
            return accessToken;
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Không lấy được access token FCM: " + e.getResponseBodyAsString(), e);
        }
    }

    private FirebaseServiceAccount loadServiceAccount() {
        if (properties.hasServiceAccountJson()) {
            return parseServiceAccount(properties.getServiceAccountJson());
        }

        if (properties.hasServiceAccountPath()) {
            try {
                String path = properties.getServiceAccountPath();
                String json;
                if (path.startsWith("classpath:")) {
                    String classpathLocation = path.substring("classpath:".length());
                    Resource resource = new ClassPathResource(classpathLocation);
                    json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                } else {
                    json = Files.readString(Path.of(path), StandardCharsets.UTF_8);
                }
                return parseServiceAccount(json);
            } catch (IOException e) {
                throw new IllegalStateException("Không đọc được file service account FCM", e);
            }
        }

        throw new IllegalStateException("Thiếu cấu hình FCM service account JSON hoặc path");
    }

    private FirebaseServiceAccount parseServiceAccount(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return new FirebaseServiceAccount(
                    text(node, "project_id"),
                    text(node, "client_email"),
                    text(node, "private_key"),
                    text(node, "token_uri")
            );
        } catch (IOException e) {
            throw new IllegalStateException("Service account FCM không hợp lệ", e);
        }
    }

    private String createJwtAssertion(FirebaseServiceAccount account) {
        try {
            long now = Instant.now().getEpochSecond();
            Map<String, Object> header = Map.of("alg", "RS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("iss", account.clientEmail());
            payload.put("sub", account.clientEmail());
            payload.put("aud", account.tokenUri());
            payload.put("scope", SCOPE);
            payload.put("iat", now);
            payload.put("exp", now + 3600);

            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = sign(signingInput, account.privateKey());
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được JWT assertion cho FCM", e);
        }
    }

    private String sign(String input, String privateKeyPem) throws Exception {
        PrivateKey privateKey = parsePrivateKey(privateKeyPem);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(input.getBytes(StandardCharsets.UTF_8));
        return base64Url(signature.sign());
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        String normalized = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record CachedToken(String token, Instant expiresAt) {
    }

    private record FirebaseServiceAccount(String projectId, String clientEmail, String privateKey, String tokenUri) {
    }
}
