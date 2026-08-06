package com.example.salonflow.ai.dto.noshow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNoShowModelConfigDto {

    private BigDecimal beta0;
    private BigDecimal beta1;
    private BigDecimal beta2;
    private BigDecimal beta3;
    private BigDecimal beta4;

    private BigDecimal riskThreshold;
    private Boolean autoSendReminder;
    private String description;
}
