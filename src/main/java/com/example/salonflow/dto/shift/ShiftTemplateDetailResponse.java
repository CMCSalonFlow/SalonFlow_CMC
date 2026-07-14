package com.example.salonflow.dto.shift;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class ShiftTemplateDetailResponse {

    private Long id;
    private Integer dayOfWeek;
    private String dayName; // "Thứ 2", "Thứ 3"...
    private LocalTime startTime;
    private LocalTime endTime;
}
