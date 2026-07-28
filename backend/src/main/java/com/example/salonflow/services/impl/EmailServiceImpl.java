package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.notification.mail.EmailProvider;
import com.example.salonflow.services.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final List<EmailProvider> emailProviders;

    @Value("${mail.primary-provider:resend}")
    private String primaryProvider;

    @Value("${mail.fallback-provider:sendgrid}")
    private String fallbackProvider;

    @Value("${mail.max-retries:3}")
    private int maxRetries;

    @Override
    public void sendVerificationOtp(String email, String otp) {
        sendNotificationEmail(
                email,
                "Xác thực email SalonFlow",
                """
                <div style="font-family:Arial,Helvetica,sans-serif;padding:24px;color:#2c221d;">
                  <h2 style="margin:0 0 12px;">SalonFlow</h2>
                  <p>OTP của bạn:</p>
                  <div style="font-size:32px;font-weight:700;letter-spacing:4px;">%s</div>
                  <p>Hết hạn sau 5 phút.</p>
                </div>
                """.formatted(otp)
        );
    }

    @Override
    public void sendResetPasswordEmail(String email, String resetLink) {
        sendNotificationEmail(
                email,
                "Đặt lại mật khẩu SalonFlow",
                """
                <div style="font-family:Arial,Helvetica,sans-serif;padding:24px;color:#2c221d;">
                  <h2>Reset Password</h2>
                  <p><a href="%s">Nhấn vào đây để đặt lại mật khẩu</a></p>
                  <p>Hết hạn sau 30 phút.</p>
                </div>
                """.formatted(resetLink)
        );
    }

    @Override
    public void sendCancellationEmail(Booking booking, CancellationResult result) {
        if (booking == null || booking.getCustomer() == null || booking.getCustomer().getEmail() == null) {
            return;
        }

        String subject = "Thông báo hủy lịch hẹn #" + booking.getId();
        String body = """
                <div style="font-family:Arial,Helvetica,sans-serif;padding:24px;color:#2c221d;">
                  <h2>Thông báo hủy lịch</h2>
                  <p>Lịch hẹn của bạn đã bị hủy.</p>
                  <p><b>Lý do:</b> %s</p>
                  <p><b>Phí hủy:</b> %s VND</p>
                </div>
                """.formatted(
                result != null && result.isFreeCancel() ? "Hủy miễn phí" : "Có phí hủy",
                result != null && result.getFeeAmount() != null ? result.getFeeAmount() : 0
        );

        sendNotificationEmail(booking.getCustomer().getEmail(), subject, body);
    }

    @Override
    public void sendOverdueCancellationEmail(Booking booking) {
        if (booking == null || booking.getCustomer() == null || booking.getCustomer().getEmail() == null) {
            return;
        }

        String subject = "Lịch hẹn #" + booking.getId() + " đã bị hủy tự động";
        String body = """
                <div style="font-family:Arial,Helvetica,sans-serif;padding:24px;color:#2c221d;">
                  <h2>Thông báo</h2>
                  <p>Lịch hẹn của bạn đã bị hủy vì quá hạn thanh toán.</p>
                </div>
                """;

        sendNotificationEmail(booking.getCustomer().getEmail(), subject, body);
    }

    @Override
    public void sendInvoiceEmail(Booking booking, String invoiceUrl) {
        if (booking == null || booking.getCustomer() == null || booking.getCustomer().getEmail() == null) {
            return;
        }

        String recipientEmail = booking.getCustomer().getEmail().trim();
        if (recipientEmail.isBlank() || recipientEmail.endsWith("@walkin.local") || recipientEmail.endsWith("@guest.local")) {
            log.info("Skip sending invoice email to dummy guest/walkin email: {}", recipientEmail);
            return;
        }

        String subject = "Thanh toán thành công - SalonFlow";
        String body = """
                <div style="font-family:Arial,Helvetica,sans-serif;padding:24px;color:#2c221d;">
                  <h2>Cảm ơn bạn đã sử dụng SalonFlow</h2>
                  <p>Thanh toán của bạn đã thành công.</p>
                  <p><b>Mã lịch hẹn:</b> %d</p>
                  <p><b>Tổng tiền:</b> %s VND</p>
                  <p><a href="%s">Tải hóa đơn PDF</a></p>
                </div>
                """.formatted(
                booking.getId(),
                booking.getTotalPrice(),
                invoiceUrl
        );

        sendNotificationEmail(recipientEmail, subject, body);
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String body) {
        sendWithRetryAndFallback(to, subject, body);
    }

    private void sendWithRetryAndFallback(String to, String subject, String body) {
        List<EmailProvider> orderedProviders = emailProviders.stream()
                .sorted(Comparator.comparingInt(provider -> providerPriority(provider.getName())))
                .toList();

        Throwable lastFailure = null;
        for (EmailProvider provider : orderedProviders) {
            if (!provider.isConfigured()) {
                continue;
            }

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    provider.send(to, subject, body);
                    return;
                } catch (Exception ex) {
                    lastFailure = ex;
                    log.warn(
                            "Email send failed via {} on attempt {}/{} for {}: {}",
                            provider.getName(),
                            attempt,
                            maxRetries,
                            to,
                            ex.getMessage()
                    );
                    if (attempt < maxRetries) {
                        sleepBackoff(attempt);
                    }
                }
            }

            log.warn("Provider {} exhausted retries, trying fallback if available", provider.getName());
        }

        throw new RuntimeException("Không thể gửi email sau " + maxRetries + " lần thử trên mọi provider", lastFailure);
    }

    private int providerPriority(String providerName) {
        String name = providerName == null ? "" : providerName.toLowerCase(Locale.ROOT);
        if (name.equals(primaryProvider == null ? "" : primaryProvider.toLowerCase(Locale.ROOT))) {
            return 0;
        }
        if (name.equals(fallbackProvider == null ? "" : fallbackProvider.toLowerCase(Locale.ROOT))) {
            return 1;
        }
        return 10;
    }

    private void sleepBackoff(int attempt) {
        long delayMs = switch (attempt) {
            case 1 -> 1000L;
            case 2 -> 3000L;
            default -> 5000L;
        };

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
