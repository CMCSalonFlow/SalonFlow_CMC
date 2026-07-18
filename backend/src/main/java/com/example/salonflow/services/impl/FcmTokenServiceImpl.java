package com.example.salonflow.services.impl;

import com.example.salonflow.dto.notification.FcmTokenRegisterRequest;
import com.example.salonflow.dto.notification.FcmTokenResponse;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.UserFcmToken;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.UserFcmTokenRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.services.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmTokenServiceImpl implements FcmTokenService {

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FcmTokenResponse registerToken(Long userId, FcmTokenRegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        UserFcmToken token = userFcmTokenRepository.findByToken(request.getToken())
                .orElseGet(UserFcmToken::new);

        token.setUser(user);
        token.setToken(request.getToken().trim());
        token.setDeviceName(request.getDeviceName());
        token.setPlatform(request.getPlatform());
        token.setIsActive(true);
        token.setLastSeenAt(Instant.now());

        token = userFcmTokenRepository.save(token);
        return toResponse(token);
    }

    @Override
    @Transactional
    public void revokeToken(Long userId, String tokenValue) {
        userFcmTokenRepository.findByUserIdAndToken(userId, tokenValue)
                .ifPresent(token -> {
                    token.setIsActive(false);
                    token.setLastSeenAt(Instant.now());
                    userFcmTokenRepository.save(token);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<FcmTokenResponse> getMyTokens(Long userId) {
        return userFcmTokenRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserFcmToken> getActiveTokensForUser(Long userId) {
        return userFcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
    }

    private FcmTokenResponse toResponse(UserFcmToken token) {
        return FcmTokenResponse.builder()
                .id(token.getId())
                .userId(token.getUser() != null ? token.getUser().getId() : null)
                .token(token.getToken())
                .deviceName(token.getDeviceName())
                .platform(token.getPlatform())
                .isActive(token.getIsActive())
                .lastSeenAt(token.getLastSeenAt())
                .createdAt(token.getCreatedAt())
                .updatedAt(token.getUpdatedAt())
                .build();
    }
}
