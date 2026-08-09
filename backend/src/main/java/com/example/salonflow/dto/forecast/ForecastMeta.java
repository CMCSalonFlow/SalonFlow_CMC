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
public class ForecastMeta {
    private Integer months;
    private Integer periods;
    private Instant lastTrainedAt;
    private Integer trainingMonths;
    private Integer dataPoints;
    private String modelVersion;
}
