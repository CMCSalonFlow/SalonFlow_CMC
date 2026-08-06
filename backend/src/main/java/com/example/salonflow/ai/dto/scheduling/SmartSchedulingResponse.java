package com.example.salonflow.ai.dto.scheduling;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSchedulingResponse {

    private Long logId;
    private Long branchId;
    private String date;
    private SmartSchedulingConfigDto weightsUsed;
    private List<SlotRecommendationDto> recommendations;
}
