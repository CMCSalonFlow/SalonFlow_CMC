package com.example.salonflow.ai.dto.noshow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoShowPredictionDto {

    private Long logId;
    private Long bookingId;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long branchId;

    private Double probability; // 0.0 -> 1.0 (ví dụ 0.784)
    private Double probabilityPercentage; // 0.0 -> 100.0 (ví dụ 78.4%)
    private String riskLevel; // LOW, MEDIUM, HIGH

    private NoShowFeaturesDto features;
    private String explanation; // Giải thích lý do dự đoán

    private Boolean isWarningTriggered;
    private Boolean smsSent;
    private LocalDateTime smsSentAt;
    private String createdAt;
}
