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

    @Override
    public void sendSalonApprovedEmail(String toEmail, String salonName, String ownerName) {
        if (toEmail == null || toEmail.isBlank()) return;
        String subject = "Chúc mừng! Đơn đăng ký Salon " + salonName + " đã được phê duyệt";
        String body = """
                <div style="font-family:Arial,Helvetica,sans-serif;padding:24px;color:#2c221d;background-color:#f9f6f0;border-radius:12px;">
                  <h2 style="color:#d4af37;margin-top:0;">SalonFlow - Phê Duyệt Salon Thành Công</h2>
                  <p>Xin chào <b>%s</b>,</p>
                  <p>Chúng tôi xin vui mừng thông báo đơn đăng ký mở salon <b>%s</b> của bạn đã được Super Admin xét duyệt thành công!</p>
                  <p>Bây giờ bạn đã có thể đăng nhập vào hệ thống quản trị Salon Owner để tạo các chi nhánh, thêm nhân viên, cài đặt bảng giá dịch vụ và bắt đầu đón khách hàng.</p>
                  <div style="margin:24px 0;">
                    <a href="http://localhost:5173/login" style="background-color:#d4af37;color:#ffffff;padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:bold;display:inline-block;">Trang Đăng Nhập Salon Owner</a>
                  </div>
                  <p>Cảm ơn bạn đã đồng hành cùng SalonFlow!</p>
                </div>
                """.formatted(ownerName != null ? ownerName : "Chủ Salon", salonName);
        sendNotificationEmail(toEmail, subject, body);
    }

    @Override
    public void sendSalonRejectedEmail(String toEmail, String salonName, String ownerName, String reason) {
        if (toEmail == null || toEmail.isBlank()) return;
        String subject = "Thông báo về đơn đăng ký Salon " + salonName;
        String body = """
                <div style="font-family:Arial,Helvetica,sans-serif;padding:24px;color:#2c221d;background-color:#fff5f5;border-radius:12px;">
                  <h2 style="color:#e53e3e;margin-top:0;">SalonFlow - Kết Quả Xét Duyệt Salon</h2>
                  <p>Xin chào <b>%s</b>,</p>
                  <p>Rất tiếc, đơn đăng ký mở salon <b>%s</b> của bạn chưa thể được phê duyệt vào lúc này.</p>
                  <div style="background-color:#ffffff;padding:16px;border-left:4px solid #e53e3e;margin:16px 0;border-radius:4px;">
                    <p style="margin:0;font-weight:bold;color:#e53e3e;">Lý do từ chối:</p>
                    <p style="margin:8px 0 0 0;">%s</p>
                  </div>
                  <p>Theo quy định của hệ thống, bạn có thể kiểm tra và cập nhật lại thông tin hồ sơ để gửi đơn **Appeal (Khởi tạo xét duyệt lại)** sau <b>7 ngày</b> kể từ ngày nhận thông báo này.</p>
                  <p>Nếu có thắc mắc, vui lòng liên hệ với đội ngũ hỗ trợ SalonFlow.</p>
                </div>
                """.formatted(ownerName != null ? ownerName : "Chủ Salon", salonName, reason != null ? reason : "Thông tin chưa đáp ứng tiêu chuẩn.");
        sendNotificationEmail(toEmail, subject, body);
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

    @Override
    public void sendWeeklyReportEmail(String toEmail, String ownerName, String salonName, java.util.Map<String, Object> reportData) {
        String subject = "📊 [SalonFlow] Báo Cáo Hiệu Suất & Doanh Thu Tuần Mới - " + salonName;
        String htmlBody = """
        <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif;max-width:640px;margin:0 auto;padding:24px;background:#f8fafc;border-radius:16px;color:#1e293b;">
          <div style="background:linear-gradient(135deg, #4f46e5, #7c3aed);padding:24px;border-radius:12px;color:#ffffff;text-align:center;">
            <h1 style="margin:0;font-size:24px;font-weight:800;">✂️ SalonFlow Analytics</h1>
            <p style="margin:6px 0 0 0;opacity:0.9;font-size:14px;">Báo Báo Hiệu Suất Hàng Tuần (Gửi tự động 8:00 AM Thứ 2)</p>
          </div>

          <div style="background:#ffffff;padding:24px;border-radius:12px;margin-top:20px;box-shadow:0 4px 12px rgba(0,0,0,0.04);">
            <p style="font-size:16px;margin:0 0 16px 0;">Kính gửi <strong>%s</strong> (Salon: <strong>%s</strong>),</p>
            <p style="font-size:14px;color:#475569;line-height:1.6;">Dưới đây là tổng hợp kết quả hoạt động kinh doanh tuần qua của hệ thống SalonFlow:</p>

            <table style="width:100%%;border-collapse:collapse;margin:20px 0;font-size:14px;">
              <tr style="border-bottom:1px solid #e2e8f0;">
                <td style="padding:12px 0;color:#64748b;">📅 Kỳ báo cáo:</td>
                <td style="padding:12px 0;font-weight:700;text-align:right;">Tuần qua (Thứ 2 đến Chủ nhật)</td>
              </tr>
              <tr style="border-bottom:1px solid #e2e8f0;">
                <td style="padding:12px 0;color:#64748b;">💰 Tổng doanh thu:</td>
                <td style="padding:12px 0;font-weight:700;color:#4f46e5;font-size:18px;text-align:right;">%s</td>
              </tr>
              <tr style="border-bottom:1px solid #e2e8f0;">
                <td style="padding:12px 0;color:#64748b;">Check-in thành công:</td>
                <td style="padding:12px 0;font-weight:700;color:#10b981;text-align:right;">%s lịch hẹn</td>
              </tr>
              <tr style="border-bottom:1px solid #e2e8f0;">
                <td style="padding:12px 0;color:#64748b;">🥇 Stylist xuất sắc nhất:</td>
                <td style="padding:12px 0;font-weight:700;color:#f59e0b;text-align:right;">%s</td>
              </tr>
            </table>

            <div style="background:#f1f5f9;padding:16px;border-radius:10px;text-align:center;margin-top:20px;">
              <p style="margin:0;font-size:13px;color:#475569;">💡 Bạn có thể truy cập hệ thống để xuất file Excel/PDF chi tiết tại:</p>
              <p style="margin:8px 0 0 0;"><a href="http://localhost:5173/reports" style="color:#4f46e5;font-weight:700;text-decoration:none;">👉 Trang Quản Lý & Xuất Báo Cáo SalonFlow</a></p>
            </div>
          </div>

          <div style="text-align:center;margin-top:20px;font-size:12px;color:#94a3b8;">
            © 2026 SalonFlow Smart Management System. Trân trọng cảm ơn!
          </div>
        </div>
        """.formatted(
                ownerName != null ? ownerName : "Chủ Salon",
                salonName != null ? salonName : "Salon",
                reportData.getOrDefault("totalRevenueStr", "0 đ"),
                reportData.getOrDefault("completedBookings", "0"),
                reportData.getOrDefault("topStaffName", "Chưa có")
        );

        sendNotificationEmail(toEmail, subject, htmlBody);
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
