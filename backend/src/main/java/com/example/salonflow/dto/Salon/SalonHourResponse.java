package com.example.salonflow.dto.Salon;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonHourResponse {

    private Integer dayOfWeek;

    private LocalTime openTime;

    private LocalTime closeTime;

    private Boolean isClosed;
}