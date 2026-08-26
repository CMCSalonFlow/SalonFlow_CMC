package com.example.salonflow.ai.controller;

import com.example.salonflow.ai.dto.scheduling.SlotRecommendationDto;
import com.example.salonflow.ai.dto.scheduling.SmartSchedulingRequestDto;
import com.example.salonflow.ai.dto.scheduling.UpdateSmartSchedulingConfigDto;
import com.example.salonflow.ai.service.SmartSchedulingService;
import com.example.salonflow.entity.SmartSchedulingLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/smart-scheduling")
@RequiredArgsConstructor
public class SmartSchedulingController {

    private final SmartSchedulingService smartSchedulingService;

    @PostMapping("/recommend")
    public ResponseEntity<List<SlotRecommendationDto>> recommendSmartSlots(
            @RequestBody SmartSchedulingRequestDto request
    ) {
        List<SlotRecommendationDto> result = smartSchedulingService.recommendTopSlots(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/config")
    public ResponseEntity<UpdateSmartSchedulingConfigDto> getConfig(
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(smartSchedulingService.getConfig(branchId));
    }

    @PutMapping("/config")
    public ResponseEntity<UpdateSmartSchedulingConfigDto> updateConfig(
            @RequestParam(required = false) Long branchId,
            @RequestBody UpdateSmartSchedulingConfigDto dto
    ) {
        return ResponseEntity.ok(smartSchedulingService.updateConfig(branchId, dto));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<SmartSchedulingLog>> getLogs(
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(smartSchedulingService.getLogs(branchId));
    }
}
