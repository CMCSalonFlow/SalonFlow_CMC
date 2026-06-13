package com.example.salonflow.services.impl;

import com.example.salonflow.services.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl
        implements EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from}")
    private String from;

    private final WebClient webClient;

    @Override
    public void sendVerificationOtp(
            String email,
            String otp
    ) {

        Map<String, Object> body =
                Map.of(
                        "from", from,
                        "to", email,
                        "subject", "Verify Email",
                        "html",
                        """
                        <h2>SalonFlow</h2>
                        <p>OTP của bạn:</p>
                        <h1>%s</h1>
                        <p>Hết hạn sau 5 phút.</p>
                        """.formatted(otp)
                );

        send(body);
    }

    @Override
    public void sendResetPasswordEmail(
            String email,
            String resetLink
    ) {

        Map<String, Object> body =
                Map.of(
                        "from", from,
                        "to", email,
                        "subject", "Reset Password",
                        "html",
                        """
                        <h2>Reset Password</h2>

                        <a href="%s">
                        Reset Password
                        </a>

                        <p>Hết hạn sau 30 phút.</p>
                        """.formatted(resetLink)
                );

        send(body);
    }

    private void send(Map<String, Object> body) {

    if (apiKey == null || apiKey.isBlank()) {
        throw new RuntimeException("Thiếu cấu hình Resend API key");
    }

    webClient.post()
            .uri("/emails")
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .block();
}
}