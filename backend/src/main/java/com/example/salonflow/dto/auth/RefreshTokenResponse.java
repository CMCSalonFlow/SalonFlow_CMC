package com.example.salonflow.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken;
}