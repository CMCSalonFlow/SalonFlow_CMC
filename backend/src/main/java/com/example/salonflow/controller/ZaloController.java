package com.example.salonflow.controller;

import com.example.salonflow.config.ZaloProperties;
import com.example.salonflow.entity.User;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.services.service.ZaloZnsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/zalo")
@RequiredArgsConstructor
@Slf4j
public class ZaloController {

    private final ZaloProperties zaloProperties;
    private final ZaloZnsService zaloZnsService;
    private final UserRepository userRepository;

    @GetMapping("/connect-url")
    public ResponseEntity<Map<String, String>> getConnectUrl() {
        String authUrl = "https://oauth.zaloapp.com/v4/oa/permission?app_id=" + zaloProperties.getAppId()
                + "&redirect_uri=http://localhost:5173/profile/zalo-callback";
        
        Map<String, String> response = new HashMap<>();
        response.put("url", authUrl);
        response.put("appId", zaloProperties.getAppId());
        response.put("oaId", zaloProperties.getOaId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connectZaloAccount(
            Authentication authentication,
            @RequestBody ConnectZaloRequest request) {

        Map<String, Object> response = new HashMap<>();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Người dùng chưa đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByUsername(username).orElse(null));

        if (user == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.badRequest().body(response);
        }

        String zaloUserId = request.getZaloUserId() != null && !request.getZaloUserId().isEmpty() ?
                request.getZaloUserId() : "zalo_uid_" + System.currentTimeMillis();

        user.setZaloUserId(zaloUserId);
        userRepository.save(user);

        log.info("Successfully connected Zalo User ID {} for user {}", zaloUserId, user.getEmail());

        response.put("success", true);
        response.put("message", "Tích hợp Zalo OA thành công!");
        response.put("zaloUserId", zaloUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-zns")
    public ResponseEntity<Map<String, Object>> sendTestZns(@RequestBody TestZnsRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        String phone = request.getPhone() != null && !request.getPhone().isEmpty() ?
                request.getPhone() : zaloProperties.getTestPhone();
        String customerName = request.getCustomerName() != null && !request.getCustomerName().isEmpty() ?
                request.getCustomerName() : "Khách Hàng Test";

        boolean sent = zaloZnsService.sendTestZns(phone, request.getTemplateId(), customerName);

        response.put("success", sent);
        response.put("message", sent ? "Đã phát tin nhắn ZNS thử nghiệm thành công!" : "Gửi tin nhắn ZNS thất bại hoặc đã được ghi log giả lập.");
        response.put("phone", phone);
        response.put("mockMode", zaloProperties.isMockEnable());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleZaloWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received Zalo OA Webhook event: {}", payload);
        
        // Auto-extract user ID or event if user follows OA or shares phone
        if (payload != null && payload.containsKey("event_name")) {
            String eventName = (String) payload.get("event_name");
            log.info("Zalo OA Event Name: {}", eventName);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }

    @Data
    public static class ConnectZaloRequest {
        private String zaloUserId;
        private String authorizationCode;
    }

    @Data
    public static class TestZnsRequest {
        private String phone;
        private String templateId;
        private String customerName;
    }
}
