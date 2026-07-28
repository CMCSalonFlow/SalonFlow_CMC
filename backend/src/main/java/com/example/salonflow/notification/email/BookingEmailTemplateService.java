package com.example.salonflow.notification.email;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.BookingItem;
import com.example.salonflow.entity.SalonService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingEmailTemplateService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingQrCodeService qrCodeService;

    @Value("${frontend.url:}")
    private String frontendUrl;

    public String renderBookingConfirmation(Booking booking) {
        String qrCode = qrCodeService.generateDataUrl(booking);
        return renderShell(
                "Xác nhận đặt lịch thành công",
                "Booking đã được tạo và đang chờ bạn đến salon.",
                booking,
                qrCode,
                "Xem chi tiết lịch hẹn",
                "Vui lòng đến đúng giờ để salon phục vụ bạn tốt nhất."
        );
    }

    public String renderAppointmentReminder(Booking booking) {
        String qrCode = qrCodeService.generateDataUrl(booking);
        return renderShell(
                "Nhắc lịch hẹn trong 24 giờ tới",
                "Salon gửi bạn lời nhắc để không bỏ lỡ lịch hẹn đã đặt.",
                booking,
                qrCode,
                "Xác nhận lại lịch hẹn",
                "Nếu bạn cần đổi lịch, vui lòng liên hệ salon sớm nhất có thể."
        );
    }

    public String renderBookingCancellation(Booking booking, String reason) {
        String qrCode = qrCodeService.generateDataUrl(booking);
        String reasonText = (reason == null || reason.isBlank()) ? "Không có ghi chú bổ sung" : reason;
        return renderShell(
                "Thông báo hủy lịch hẹn",
                "Lịch hẹn của bạn đã được cập nhật trạng thái hủy.",
                booking,
                qrCode,
                "Xem lại lịch khác",
                "Lý do hủy: " + reasonText
        );
    }

    private String renderShell(
            String heading,
            String intro,
            Booking booking,
            String qrCode,
            String ctaLabel,
            String note
    ) {
        String services = renderServices(booking);
        String bookingCode = booking != null ? "#BK" + booking.getId() : "#BK";
        String dateText = booking != null && booking.getBookingDate() != null ? booking.getBookingDate().format(DATE_FORMAT) : "-";
        String timeText = booking != null && booking.getStartTime() != null ? booking.getStartTime().format(TIME_FORMAT) : "-";
        String branchName = booking != null && booking.getBranch() != null ? booking.getBranch().getName() : "-";
        String staffName = booking != null && booking.getAssignedStaff() != null ? booking.getAssignedStaff().getName() : "Chưa phân công";
        String customerName = booking != null && booking.getCustomer() != null ? booking.getCustomer().getFullName() : "-";
        String totalPrice = formatMoney(booking != null ? booking.getTotalPrice() : null);
        String deposit = formatMoney(booking != null ? booking.getDepositAmount() : null);
        String remaining = formatMoney(booking != null ? booking.getRemainingAmount() : null);
        String bookingLink = buildBookingLink(booking);

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <meta name="x-apple-disable-message-reformatting">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#f6efe9;font-family:Arial,Helvetica,sans-serif;color:#2c221d;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#f6efe9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="680" cellspacing="0" cellpadding="0" border="0" style="width:680px;max-width:680px;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 14px 40px rgba(72,46,32,.12);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#2f1f1a 0%%,#8b5e3c 100%%);padding:32px 40px;color:#fff;">
                              <div style="font-size:13px;letter-spacing:1.8px;text-transform:uppercase;opacity:.88;">SalonFlow</div>
                              <div style="font-size:30px;line-height:1.2;font-weight:700;margin-top:10px;">%s</div>
                              <div style="font-size:15px;line-height:1.6;margin-top:10px;max-width:520px;opacity:.96;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:40px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="vertical-align:top;padding-right:20px;">
                                    <div style="font-size:14px;color:#8b5e3c;font-weight:700;text-transform:uppercase;letter-spacing:.6px;">Mã lịch hẹn</div>
                                    <div style="font-size:28px;font-weight:700;margin:10px 0 18px;color:#2c221d;">%s</div>
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="font-size:14px;line-height:1.8;">
                                      <tr><td style="color:#7c6a61;width:160px;">Khách hàng</td><td style="font-weight:700;">%s</td></tr>
                                      <tr><td style="color:#7c6a61;">Chi nhánh</td><td style="font-weight:700;">%s</td></tr>
                                      <tr><td style="color:#7c6a61;">Nhân viên</td><td style="font-weight:700;">%s</td></tr>
                                      <tr><td style="color:#7c6a61;">Ngày</td><td style="font-weight:700;">%s</td></tr>
                                      <tr><td style="color:#7c6a61;">Giờ</td><td style="font-weight:700;">%s</td></tr>
                                      <tr><td style="color:#7c6a61;">Tổng tiền</td><td style="font-weight:700;">%s VND</td></tr>
                                      <tr><td style="color:#7c6a61;">Đã cọc</td><td style="font-weight:700;">%s VND</td></tr>
                                      <tr><td style="color:#7c6a61;">Còn lại</td><td style="font-weight:700;">%s VND</td></tr>
                                    </table>
                                  </td>
                                  <td style="width:210px;vertical-align:top;text-align:center;">
                                    <div style="font-size:13px;font-weight:700;color:#7c6a61;text-transform:uppercase;letter-spacing:.6px;margin-bottom:12px;">QR code</div>
                                    <div style="padding:14px;border:1px solid #ead9cc;border-radius:18px;background:#fff;">
                                      <img src="%s" width="220" height="220" alt="QR code booking" style="display:block;width:220px;height:220px;margin:0 auto;border-radius:12px;">
                                    </div>
                                  </td>
                                </tr>
                              </table>
                              <div style="margin-top:28px;padding:18px 20px;background:#f9f4ef;border-radius:16px;border:1px solid #eddccb;">
                                <div style="font-size:13px;color:#8b5e3c;font-weight:700;text-transform:uppercase;letter-spacing:.6px;margin-bottom:8px;">Dịch vụ</div>
                                <div style="font-size:15px;line-height:1.8;color:#3a2a23;">%s</div>
                              </div>
                              <div style="margin-top:22px;font-size:15px;line-height:1.8;color:#4b3a33;">
                                <strong>%s</strong>
                              </div>
                              <div style="margin-top:22px;text-align:center;">
                                <a href="%s" style="display:inline-block;background:#8b5e3c;color:#fff;text-decoration:none;padding:14px 26px;border-radius:999px;font-size:15px;font-weight:700;">%s</a>
                              </div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 40px 36px;">
                              <div style="border-top:1px solid #eee0d5;padding-top:18px;font-size:13px;line-height:1.7;color:#7c6a61;">
                                Email này được gửi tự động bởi SalonFlow. Nếu bạn cần hỗ trợ, vui lòng liên hệ salon của bạn.
                              </div>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                heading,
                heading,
                intro,
                bookingCode,
                customerName,
                branchName,
                staffName,
                dateText,
                timeText,
                totalPrice,
                deposit,
                remaining,
                qrCode,
                services,
                note,
                bookingLink,
                ctaLabel
        );
    }

    private String buildBookingLink(Booking booking) {
        if (booking == null || booking.getId() == null || frontendUrl == null || frontendUrl.isBlank()) {
            return "#";
        }
        return frontendUrl.replaceAll("/+$", "") + "/bookings/" + booking.getId();
    }

    private String renderServices(Booking booking) {
        if (booking == null || booking.getItems() == null || booking.getItems().isEmpty()) {
            return "Chưa có dịch vụ";
        }

        return booking.getItems().stream()
                .map(this::renderItem)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("<br>"));
    }

    private String renderItem(BookingItem item) {
        if (item == null) {
            return null;
        }

        SalonService service = item.getService();
        if (service != null && service.getName() != null) {
            return "• " + service.getName() + " (" + item.getDurationMinutes() + " phút)";
        }

        if (item.getBundle() != null && item.getBundle().getName() != null) {
            return "• " + item.getBundle().getName() + " (" + item.getDurationMinutes() + " phút)";
        }

        return "• Dịch vụ đặt lịch";
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
