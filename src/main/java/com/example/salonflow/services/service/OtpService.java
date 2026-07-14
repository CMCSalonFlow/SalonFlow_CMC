package com.example.salonflow.services.service;

public interface OtpService {

    void saveOtp(
            String email,
            String otp
    );

    String getOtp(
            String email
    );

    void deleteOtp(
            String email
    );
}
