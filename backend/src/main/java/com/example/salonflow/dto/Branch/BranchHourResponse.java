package com.example.salonflow.dto.Branch;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchHourResponse {

    private Integer dayOfWeek;

    private LocalTime openTime;

    private LocalTime closeTime;

    private Boolean isClosed;
}
