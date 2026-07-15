package com.example.salonflow.services.service;

import com.example.salonflow.dto.auth.*;
import com.example.salonflow.dto.common.MessageResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface AuthenticationService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse loginWithOAuth2(
            String registrationId,
            OAuth2User oauth2User
    );

    RefreshTokenResponse refreshToken(
            RefreshTokenRequest request);

    void logout(Long userId);

    void sendVerificationOtp(
            String email);

    MessageResponse verifyEmail(
            String email,
            String otp);

    void forgotPassword(
            String email);

    void resetPassword(
            String token,
            String newPassword);
}
