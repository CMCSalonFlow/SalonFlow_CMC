package com.example.salonflow.services.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

        String generateToken(UserDetails userDetails);

        String extractUsername(String token);

        boolean isTokenValid(String token, UserDetails userDetails);

        String generateResetPasswordToken(String email);

        String extractEmailFromResetToken(String token);

        boolean isResetTokenValid(String token);
}