package com.example.salonflow.services.impl;

import com.example.salonflow.config.ZaloProperties;
import com.example.salonflow.entity.ZaloToken;
import com.example.salonflow.repository.ZaloTokenRepository;
import com.example.salonflow.services.service.ZaloTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloTokenServiceImpl implements ZaloTokenService {

    private final ZaloProperties zaloProperties;
    private final ZaloTokenRepository zaloTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public String getValidAccessToken() {
        if (zaloProperties.isMockEnable()) {
            return "mock_zalo_access_token_123456789";
        }

        Optional<ZaloToken> tokenOpt = zaloTokenRepository.findByOaId(zaloProperties.getOaId());
        if (tokenOpt.isPresent()) {
            ZaloToken token = tokenOpt.get();
            // Check if token expires in less than 5 minutes
            if (token.getExpiresAt().isAfter(Instant.now().plusSeconds(300))) {
                return token.getAccessToken();
            }
        }

        return refreshAccessToken();
    }

    @Override
    @Transactional
    public String refreshAccessToken() {
        if (zaloProperties.isMockEnable()) {
            log.info("[ZALO MOCK TOKEN] Returning mock access token for OA ID: {}", zaloProperties.getOaId());
            return "mock_zalo_access_token_123456789";
        }

        try {
            log.info("Refreshing Zalo OA Access Token...");
            String refreshToken = zaloProperties.getRefreshToken();
            Optional<ZaloToken> tokenOpt = zaloTokenRepository.findByOaId(zaloProperties.getOaId());
            if (tokenOpt.isPresent() && tokenOpt.get().getRefreshToken() != null) {
                refreshToken = tokenOpt.get().getRefreshToken();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("secret_key", zaloProperties.getAppSecret());

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("app_id", zaloProperties.getAppId());
            map.add("grant_type", "refresh_token");
            map.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(zaloProperties.getTokenUrl(), request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("access_token")) {
                    String newAccessToken = (String) body.get("access_token");
                    String newRefreshToken = (String) body.get("refresh_token");
                    long expiresIn = Long.parseLong(body.getOrDefault("expires_in", "90000").toString());

                    ZaloToken token = tokenOpt.orElseGet(() -> ZaloToken.builder()
                            .oaId(zaloProperties.getOaId())
                            .build());

                    token.setAccessToken(newAccessToken);
                    if (newRefreshToken != null) {
                        token.setRefreshToken(newRefreshToken);
                    }
                    token.setExpiresAt(Instant.now().plusSeconds(expiresIn));

                    zaloTokenRepository.save(token);
                    log.info("Successfully refreshed Zalo OA Access Token!");
                    return newAccessToken;
                } else {
                    log.error("Failed to refresh Zalo Token. Response body: {}", body);
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred while refreshing Zalo Access Token: {}", e.getMessage(), e);
        }

        return "mock_zalo_access_token_fallback";
    }
}
