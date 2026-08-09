package com.example.salonflow.dto.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueForecastTrainResponse {
    private Long branchId;
    private String modelKey;
    private String modelPath;
    private Integer trainedPoints;
}
