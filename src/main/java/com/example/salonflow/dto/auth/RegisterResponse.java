package com.example.salonflow.dto.auth;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private String email;

    private String message;
}
