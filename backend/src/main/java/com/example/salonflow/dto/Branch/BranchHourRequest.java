package com.example.salonflow.dto.Branch;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchHourRequest {

    @Min(0)
    @Max(6)
    private Integer dayOfWeek;

    private LocalTime openTime;

    private LocalTime closeTime;

    private Boolean isClosed;
}
