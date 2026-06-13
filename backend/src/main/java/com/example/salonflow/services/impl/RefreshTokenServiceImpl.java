package com.example.salonflow.services.impl;

import com.example.salonflow.entity.RefreshToken;
import com.example.salonflow.entity.User;
import com.example.salonflow.repository.RefreshTokenRepository;
import com.example.salonflow.services.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl
        implements RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenDuration;

    @Override
    public RefreshToken createRefreshToken(User user) {

        repository.deleteByUser(user);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(
                        LocalDateTime.now()
                                .plusSeconds(
                                        refreshTokenDuration / 1000
                                )
                )
                .build();

        return repository.save(token);
    }

    @Override
    public RefreshToken verifyExpiration(
            String token
    ) {

        RefreshToken refreshToken =
                repository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid refresh token"
                                ));

        if (refreshToken.getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            repository.delete(refreshToken);

            throw new RuntimeException(
                    "Refresh token expired"
            );
        }

        return refreshToken;
    }

    @Override
    public void deleteByUser(User user) {
        repository.deleteByUser(user);
    }
}
