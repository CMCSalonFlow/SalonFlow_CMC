package com.example.salonflow.controller;

import com.example.salonflow.dto.notification.NotificationResponse;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

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
}
