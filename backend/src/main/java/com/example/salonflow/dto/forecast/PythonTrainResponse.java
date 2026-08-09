package com.example.salonflow.dto.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonTrainResponse {
    private String salonId;
    private String modelPath;
    private Integer trainedPoints;
}
