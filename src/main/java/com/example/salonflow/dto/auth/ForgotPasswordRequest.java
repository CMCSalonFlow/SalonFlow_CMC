package com.example.salonflow.dto.auth;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @Email
    private String email;
}
