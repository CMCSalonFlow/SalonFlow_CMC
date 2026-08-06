package com.example.salonflow.ai.dto.scheduling;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSchedulingConfigDto {

    private Long id;
    private Long branchId;
    private BigDecimal workloadWeight;
    private BigDecimal travelWeight;
    private BigDecimal serviceFitWeight;
    private String description;
}
