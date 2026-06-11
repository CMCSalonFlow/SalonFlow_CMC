package com.example.salonflow.controller;

import com.example.salonflow.dto.AuthResponse;
import com.example.salonflow.dto.LoginRequest;
import com.example.salonflow.dto.RegisterRequest;
import com.example.salonflow.services.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody String refreshToken) {
        return authService.refresh(refreshToken);
    }
}