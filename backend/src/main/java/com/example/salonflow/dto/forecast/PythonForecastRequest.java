package com.example.salonflow.dto.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonForecastRequest {
    private String salonId;
    private List<DailyRevenuePoint> history;
    private Integer periods;
    private Double intervalWidth;
}
