package com.example.salonflow.ai.controller;

import com.example.salonflow.ai.dto.scheduling.*;
import com.example.salonflow.ai.service.SmartSchedulingService;
import com.example.salonflow.entity.SmartSchedulingLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/smart-scheduling")
@RequiredArgsConstructor
public class SmartSchedulingController {

    private final SmartSchedulingService smartSchedulingService;

    /**
     * Đề xuất Top 3 slot tối ưu tránh xung đột cho khách hàng dựa trên Rule-based + ML scoring.
     */
    @PostMapping("/recommend")
    public ResponseEntity<SmartSchedulingResponse> recommendSlots(@Valid @RequestBody SmartSchedulingRequest request) {
        SmartSchedulingResponse response = smartSchedulingService.recommendSlots(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy cấu hình trọng số gợi ý hiện tại.
     */
    @GetMapping("/config")
    public ResponseEntity<SmartSchedulingConfigDto> getConfig(@RequestParam(required = false) Long branchId) {
        SmartSchedulingConfigDto config = smartSchedulingService.getConfig(branchId);
        return ResponseEntity.ok(config);
    }

    /**
     * Admin/Salon Owner điều chỉnh trọng số thuật toán gợi ý slot.
     */
    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER', 'BRANCH_MANAGER')")
    public ResponseEntity<SmartSchedulingConfigDto> updateConfig(
            @RequestParam(required = false) Long branchId,
            @Valid @RequestBody UpdateSmartSchedulingConfigDto dto) {
        SmartSchedulingConfigDto updated = smartSchedulingService.updateConfig(branchId, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Xem danh sách log lịch sử gợi ý slot để đánh giá hiệu quả thuật toán.
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER', 'BRANCH_MANAGER')")
    public ResponseEntity<Page<SmartSchedulingLog>> getRecommendationLogs(
            @RequestParam(required = false) Long branchId,
            Pageable pageable) {
        Page<SmartSchedulingLog> logs = smartSchedulingService.getRecommendationLogs(branchId, pageable);
        return ResponseEntity.ok(logs);
    }
}
