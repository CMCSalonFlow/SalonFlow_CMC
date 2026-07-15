package com.example.salonflow.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {

    private String token;

    private String newPassword;
}
