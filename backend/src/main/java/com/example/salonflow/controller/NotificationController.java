package com.example.salonflow.controller;

import com.example.salonflow.dto.notification.NotificationResponse;
import com.example.salonflow.dto.notification.FcmTokenRegisterRequest;
import com.example.salonflow.dto.notification.FcmTokenResponse;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.FcmTokenService;
import com.example.salonflow.services.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final FcmTokenService fcmTokenService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable("id") Long notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.markAsRead(userId, notificationId));
    }

    @GetMapping("/fcm-tokens")
    public ResponseEntity<List<FcmTokenResponse>> getMyFcmTokens() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(fcmTokenService.getMyTokens(userId));
    }

    @PostMapping("/fcm-tokens")
    public ResponseEntity<FcmTokenResponse> registerFcmToken(@RequestBody FcmTokenRegisterRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(fcmTokenService.registerToken(userId, request));
    }

    @DeleteMapping("/fcm-tokens")
    public ResponseEntity<Void> revokeFcmToken(@RequestParam String token) {
        Long userId = SecurityUtils.getCurrentUserId();
        fcmTokenService.revokeToken(userId, token);
        return ResponseEntity.noContent().build();
    }
}
