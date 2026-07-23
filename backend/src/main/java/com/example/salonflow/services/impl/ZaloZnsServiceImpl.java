package com.example.salonflow.services.impl;

import com.example.salonflow.config.ZaloProperties;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.User;
import com.example.salonflow.services.service.ZaloTokenService;
import com.example.salonflow.services.service.ZaloZnsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloZnsServiceImpl implements ZaloZnsService {

    private final ZaloProperties zaloProperties;
    private final ZaloTokenService zaloTokenService;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public boolean sendBookingCreatedZns(Booking booking, User customer) {
        String templateId = zaloProperties.getTemplate().getBookingCreated();
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name",
                customer != null && customer.getFullName() != null ? customer.getFullName() : "Khách hàng");
        templateData.put("booking_code", "BK-" + booking.getId());
        templateData.put("booking_time",
                booking.getBookingDate() != null && booking.getStartTime() != null
                        ? booking.getBookingDate().atTime(booking.getStartTime()).format(DATE_FORMATTER)
                        : "Giờ đã chọn");
        templateData.put("salon_name", booking.getBranch() != null ? booking.getBranch().getName() : "SalonFlow");
        templateData.put("salon_address",
                booking.getBranch() != null && booking.getBranch().getAddress() != null
                        ? booking.getBranch().getAddress()
                        : "Chi nhánh SalonFlow");
        templateData.put("price",
                booking.getTotalPrice() != null ? booking.getTotalPrice().toString() + " VND" : "0 VND");

        String phone = extractPhone(customer);
        return sendZnsMessage(phone, templateId, templateData, "DAT_LICH_THANH_CONG");
    }

    @Override
    public boolean sendAppointmentReminderZns(Booking booking, User customer) {
        String templateId = zaloProperties.getTemplate().getAppointmentReminder();
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name",
                customer != null && customer.getFullName() != null ? customer.getFullName() : "Khách hàng");
        templateData.put("booking_code", "BK-" + booking.getId());
        templateData.put("booking_time",
                booking.getBookingDate() != null && booking.getStartTime() != null
                        ? booking.getBookingDate().atTime(booking.getStartTime()).format(DATE_FORMATTER)
                        : "Giờ hẹn");
        templateData.put("salon_name", booking.getBranch() != null ? booking.getBranch().getName() : "SalonFlow");
        templateData.put("salon_address",
                booking.getBranch() != null && booking.getBranch().getAddress() != null
                        ? booking.getBranch().getAddress()
                        : "Chi nhánh SalonFlow");

        String phone = extractPhone(customer);
        return sendZnsMessage(phone, templateId, templateData, "NHAC_LICH_HEN");
    }

    @Override
    public boolean sendBookingCancelledZns(Booking booking, User customer, String cancelReason) {
        String templateId = zaloProperties.getTemplate().getBookingCancelled();
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name",
                customer != null && customer.getFullName() != null ? customer.getFullName() : "Khách hàng");
        templateData.put("booking_code", "BK-" + booking.getId());
        templateData.put("cancel_reason", cancelReason != null ? cancelReason : "Thay đổi kế hoạch");
        templateData.put("salon_name", booking.getBranch() != null ? booking.getBranch().getName() : "SalonFlow");

        String phone = extractPhone(customer);
        return sendZnsMessage(phone, templateId, templateData, "HUY_LICH_HEN");
    }

    @Override
    public boolean sendTestZns(String phone, String templateId, String customerName) {
        String targetTemplateId = templateId != null && !templateId.isEmpty() ? templateId
                : zaloProperties.getTemplate().getBookingCreated();
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name", customerName != null ? customerName : "Khách hàng thử nghiệm");
        templateData.put("booking_code", "TEST-9999");
        templateData.put("booking_time", "14:00 25/07/2026");
        templateData.put("salon_name", "SalonFlow Demo");
        templateData.put("salon_address", "123 Đường Demo, Quận 1, TP.HCM");

        return sendZnsMessage(phone, targetTemplateId, templateData, "TEST_ZNS");
    }

    private boolean sendZnsMessage(String rawPhone, String templateId, Map<String, Object> templateData,
            String eventName) {
        String formattedPhone = formatPhone(rawPhone);

        if (zaloProperties.isMockEnable()) {
            String customerName = String.valueOf(templateData.getOrDefault("customer_name", "Quý khách"));
            String bookingCode = String.valueOf(templateData.getOrDefault("booking_code", "-"));
            String bookingTime = String.valueOf(templateData.getOrDefault("booking_time", "-"));
            String salonName = String.valueOf(templateData.getOrDefault("salon_name", "SalonFlow"));
            String salonAddress = String.valueOf(templateData.getOrDefault("salon_address", "-"));
            String price = templateData.containsKey("price") ? String.valueOf(templateData.get("price")) : null;
            String cancelReason = templateData.containsKey("cancel_reason") ? String.valueOf(templateData.get("cancel_reason")) : null;

            StringBuilder sb = new StringBuilder();
            sb.append("\n┌─────────────────────────────────────────────────────────────┐\n");
            sb.append("│                📱 ZALO OFFICIAL ACCOUNT (ZNS)              │\n");
            sb.append("├─────────────────────────────────────────────────────────────┤\n");
            sb.append("│ Kính gửi    : ").append(customerName).append("\n");
            sb.append("│ Sự kiện     : ").append(eventName).append("\n");
            sb.append("│ SĐT Nhận    : ").append(formattedPhone).append(" (Gốc: ").append(rawPhone).append(")\n");
            sb.append("│ Template ID : ").append(templateId).append("\n");
            sb.append("├─────────────────────────────────────────────────────────────┤\n");
            sb.append("│ 💈 Salon    : ").append(salonName).append("\n");
            if (!"-".equals(salonAddress)) {
                sb.append("│ 📍 Địa chỉ  : ").append(salonAddress).append("\n");
            }
            sb.append("│ 🔖 Mã lịch  : ").append(bookingCode).append("\n");
            sb.append("│ ⏰ Thời gian: ").append(bookingTime).append("\n");
            if (price != null) {
                sb.append("│ 💰 Tổng tiền: ").append(price).append("\n");
            }
            if (cancelReason != null) {
                sb.append("│ ⚠️ Lý do hủy: ").append(cancelReason).append("\n");
            }
            sb.append("└─────────────────────────────────────────────────────────────┘");

            log.info(sb.toString());
            return true;
        }

        try {
            String accessToken = zaloTokenService.getValidAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("access_token", accessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("phone", formattedPhone);
            body.put("template_id", templateId);
            body.put("template_data", templateData);
            body.put("tracking_id", "TRK-" + System.currentTimeMillis());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(zaloProperties.getZnsSendUrl(), request,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> respBody = response.getBody();
                Object errorObj = respBody.get("error");
                int errorCode = errorObj != null ? Integer.parseInt(errorObj.toString()) : -1;

                if (errorCode == 0) {
                    log.info("✅ Gửi tin nhắn ZNS Zalo THẬT thành công tới phone={}", formattedPhone);
                    return true;
                } else {
                    log.warn("⚠️ Zalo ZNS API trả về lỗi: error={}, message={}", errorCode, respBody.get("message"));
                }
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi ZNS Zalo tới phone={}: {}", formattedPhone, e.getMessage());
        }

        // Fallback log
        log.info("🔄 [ZNS FALLBACK LOG] Sự kiện: {}, Phone: {}, Data: {}", eventName, formattedPhone, templateData);
        return false;
    }

    private String extractPhone(User customer) {
        if (customer != null && customer.getPhone() != null && !customer.getPhone().isEmpty()) {
            return customer.getPhone();
        }
        return zaloProperties.getTestPhone();
    }

    private String formatPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            phone = zaloProperties.getTestPhone() != null ? zaloProperties.getTestPhone() : "0987654321";
        }
        phone = phone.replaceAll("[^0-9]", "");
        if (phone.startsWith("0")) {
            return "84" + phone.substring(1);
        }
        if (!phone.startsWith("84")) {
            return "84" + phone;
        }
        return phone;
    }
}
