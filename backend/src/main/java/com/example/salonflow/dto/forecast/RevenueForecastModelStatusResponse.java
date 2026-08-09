package com.example.salonflow.dto.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueForecastModelStatusResponse {
    private Long branchId;
    private Boolean trained;
    private Instant lastTrainedAt;
    private Integer trainingMonths;
    private Integer dataPoints;
    private String modelVersion;
}
