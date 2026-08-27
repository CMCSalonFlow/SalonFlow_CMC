package com.example.salonflow.controller;

import com.example.salonflow.dto.subscription.SubscriptionPlanConfigResponse;
import com.example.salonflow.dto.subscription.UpdateSubscriptionPlanConfigRequest;
import com.example.salonflow.entity.enums.SubscriptionPlan;
import com.example.salonflow.services.service.SubscriptionPlanConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanConfigController {

    private final SubscriptionPlanConfigService planConfigService;

    /**
     * Endpoint công khai cho Owner / Khách xem bảng giá các gói.
     */
    @GetMapping("/public")
    public ResponseEntity<List<SubscriptionPlanConfigResponse>> getPublicPlanConfigs() {
        return ResponseEntity.ok(planConfigService.getAllConfigs());
    }

    /**
     * Endpoint dành cho Admin xem đầy đủ thông số quản trị.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<SubscriptionPlanConfigResponse>> getAdminPlanConfigs() {
        return ResponseEntity.ok(planConfigService.getAllConfigs());
    }

    /**
     * Endpoint dành cho Admin cập nhật giá tiền & giới hạn gói dịch vụ.
     */
    @PutMapping("/admin/{plan}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionPlanConfigResponse> updatePlanConfig(
            @PathVariable SubscriptionPlan plan,
            @RequestBody UpdateSubscriptionPlanConfigRequest request
    ) {
        return ResponseEntity.ok(planConfigService.updateConfig(plan, request));
    }
}
