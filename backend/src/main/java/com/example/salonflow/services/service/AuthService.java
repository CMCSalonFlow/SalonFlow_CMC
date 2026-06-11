package com.example.salonflow.services.service;

import com.example.salonflow.dto.AuthResponse;
import com.example.salonflow.dto.LoginRequest;
import com.example.salonflow.dto.RegisterRequest;
import com.example.salonflow.entity.RefreshToken;
import com.example.salonflow.entity.User;
import com.example.salonflow.repository.RefreshTokenRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        if (user.getStatus() == null) {
            user.setStatus(com.example.salonflow.entity.enums.UserStatus.ACTIVE);
        }

        userRepository.save(user);
        return new AuthResponse("REGISTER_SUCCESS", null);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshTokenStr = jwtService.generateRefreshToken(user.getEmail());

        // Lưu refresh token vào DB
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setUser(user);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshTokenStr);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token has expired");
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user.getEmail());
        String newRefreshTokenStr = jwtService.generateRefreshToken(user.getEmail());

        // Rotate refresh token (tăng bảo mật)
        refreshToken.setToken(newRefreshTokenStr);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(newAccessToken, newRefreshTokenStr);
    }
}