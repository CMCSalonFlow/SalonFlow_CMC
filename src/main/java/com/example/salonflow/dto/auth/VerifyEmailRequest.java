package com.example.salonflow.dto.auth;

import lombok.Data;

@Data
public class VerifyEmailRequest {

    private String email;

    private String otp;
}
