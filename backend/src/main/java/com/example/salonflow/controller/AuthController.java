package com.example.salonflow.controller;

import com.example.salonflow.dto.auth.*;
import jakarta.validation.Valid;
import com.example.salonflow.services.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        return ResponseEntity.ok(
                authenticationService.register(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        System.out.println("LOGIN CONTROLLER CALLED");

        return ResponseEntity.ok(
                authenticationService.login(request)
        );
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        return ResponseEntity.ok(
                authenticationService.refreshToken(
                        request
                )
        );
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<Void> logout(
            @PathVariable Long userId
    ) {

        authenticationService.logout(userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/oauth2/{provider}")
    public ResponseEntity<Void> loginWithOAuth2(
            @PathVariable String provider
    ) {

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(
                        "Location",
                        "/oauth2/authorization/" + provider
                )
                .build();
    }
}
