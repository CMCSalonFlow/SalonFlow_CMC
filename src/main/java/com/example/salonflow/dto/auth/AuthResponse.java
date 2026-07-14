package com.example.salonflow.dto.auth;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthResponse {

    private Long userId;

    private String username;

    private String email;

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private List<String> roles;
}