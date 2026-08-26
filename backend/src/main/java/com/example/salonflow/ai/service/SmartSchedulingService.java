package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.scheduling.SlotRecommendationDto;
import com.example.salonflow.ai.dto.scheduling.SmartSchedulingRequestDto;
import com.example.salonflow.ai.dto.scheduling.UpdateSmartSchedulingConfigDto;
import com.example.salonflow.entity.SmartSchedulingLog;

import java.util.List;

public interface SmartSchedulingService {
    List<SlotRecommendationDto> recommendTopSlots(SmartSchedulingRequestDto request);
    UpdateSmartSchedulingConfigDto getConfig(Long branchId);
    UpdateSmartSchedulingConfigDto updateConfig(Long branchId, UpdateSmartSchedulingConfigDto dto);
    List<SmartSchedulingLog> getLogs(Long branchId);
}
