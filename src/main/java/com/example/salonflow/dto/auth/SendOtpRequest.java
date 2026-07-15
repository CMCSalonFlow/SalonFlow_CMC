package com.example.salonflow.dto.auth;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class SendOtpRequest {

    @Email
    private String email;
}
