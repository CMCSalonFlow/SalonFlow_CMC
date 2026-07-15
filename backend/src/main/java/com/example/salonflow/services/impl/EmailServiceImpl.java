package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.services.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from}")
    private String from;

    private final WebClient webClient;

    @Override
    public void sendVerificationOtp(String email, String otp) {

        Map<String, Object> body = Map.of(
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
    public void sendResetPasswordEmail(String email, String resetLink) {

        Map<String, Object> body = Map.of(
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

    @Override
    public void sendCancellationEmail(Booking booking, CancellationResult result) {
        String subject = "Thông báo hủy lịch hẹn #" + booking.getId();

        String body = """
                <h2>Thông báo hủy lịch</h2>

                <p>Lịch hẹn của bạn đã bị hủy.</p>

                <p><b>Lý do:</b> %s</p>

                <p><b>Phí hủy:</b> %s VND</p>
                """.formatted(
                result.isFreeCancel() ? "Hủy miễn phí" : "Có phí hủy",
                result.getFeeAmount()
        );

        sendEmail(booking.getCustomer().getEmail(), subject, body);
    }

    @Override
    public void sendOverdueCancellationEmail(Booking booking) {
        String subject = "Lịch hẹn #" + booking.getId() + " đã bị hủy tự động";

        String body = """
                <h2>Thông báo</h2>

                <p>Lịch hẹn của bạn đã bị hủy vì quá hạn thanh toán.</p>
                """;

        sendEmail(booking.getCustomer().getEmail(), subject, body);
    }

    @Override
    public void sendInvoiceEmail(Booking booking, String invoiceUrl) {

        String subject = "Thanh toán thành công - SalonFlow";

        String body = """
                <h2>Cảm ơn bạn đã sử dụng SalonFlow</h2>

                <p>Thanh toán của bạn đã thành công.</p>

                <p><b>Mã lịch hẹn:</b> %d</p>

                <p><b>Tổng tiền:</b> %s VND</p>

                <p>
                    Bạn có thể tải hóa đơn tại:
                </p>

                <p>
                    <a href="%s">Tải hóa đơn PDF</a>
                </p>

                <br>

                <p>Xin cảm ơn!</p>
                """.formatted(
                booking.getId(),
                booking.getTotalPrice(),
                invoiceUrl
        );

        sendEmail(booking.getCustomer().getEmail(), subject, body);
    }

    private void sendEmail(String to, String subject, String body) {

        try {
            Map<String, Object> emailBody = Map.of(
                    "from", from,
                    "to", to,
                    "subject", subject,
                    "html", body
            );

            webClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(emailBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Email sent to: " + to);

        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}