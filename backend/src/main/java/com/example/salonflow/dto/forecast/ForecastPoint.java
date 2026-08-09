package com.example.salonflow.dto.forecast;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastPoint {
    private LocalDate date;
    private Double yhat;

    @JsonAlias("yhat_lower")
    private Double yhatLower;

    @JsonAlias("yhat_upper")
    private Double yhatUpper;
}
