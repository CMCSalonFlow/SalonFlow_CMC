package com.example.salonflow.services.impl;

import com.example.salonflow.services.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Tích hợp ESMS để gửi SMS nhắc hẹn (US-037).
 */
@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${esms.api-key}")
    private String apiKey;

    @Value("${esms.secret-key}")
    private String secretKey;

    @Value("${esms.brand-name}")
    private String brandName;

    @Value("${esms.api-url:https://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_post_json/}")
    private String apiUrl;

    @Value("${esms.mock-enable:true}")
    private boolean mockEnable;

    @Override
    public boolean sendSms(String phone, String message) {
        // Giới hạn 160 ký tự
        if (message != null && message.length() > 160) {
            message = message.substring(0, 160);
        }

        String formattedPhone = formatPhone(phone);
        if (formattedPhone == null) {
            log.warn("So dien thoai khong hop le: {}", phone);
            return false;
        }

        // Mock mode - chỉ log, không gọi API thật
        if (mockEnable) {
            log.info("=== [ESMS MOCK] ===");
            log.info("Phone  : {}", formattedPhone);
            log.info("Message: {}", message);
            log.info("===================");
            return true;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("ApiKey", apiKey);
            body.put("SecretKey", secretKey);
            body.put("Phone", formattedPhone);
            body.put("Content", message);
            body.put("SmsType", 2); // Tin nhắn brandname
            body.put("Brandname", brandName);
            body.put("IsUnicode", 0); // 0 = không unicode, 1 = unicode

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> respBody = response.getBody();
                Object codeObj = respBody.get("CodeResult");
                String code = codeObj != null ? codeObj.toString() : "";

                if ("100".equals(code)) {
                    log.info("Gui SMS ESMS thanh cong toi {}", formattedPhone);
                    return true;
                } else {
                    log.warn("ESMS tra ve loi: CodeResult={}, Message={}", code, respBody.get("ErrorMessage"));
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Loi khi gui SMS ESMS toi {}: {}", formattedPhone, e.getMessage());
        }

        return false;
    }

    /**
     * Chuẩn hóa số điện thoại về dạng 84xxxxxxxxx
     */
    private String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        phone = phone.trim().replaceAll("[^0-9]", "");
        if (phone.startsWith("0") && phone.length() == 10) {
            return "84" + phone.substring(1);
        }
        if (phone.startsWith("84") && phone.length() == 11) {
            return phone;
        }
        return null;
    }
}
