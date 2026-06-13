package com.example.salonflow.services.service;

import com.example.salonflow.dto.auth.*;

public interface AuthenticationService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    RefreshTokenResponse refreshToken(
            RefreshTokenRequest request
    );

    void logout(Long userId);
}