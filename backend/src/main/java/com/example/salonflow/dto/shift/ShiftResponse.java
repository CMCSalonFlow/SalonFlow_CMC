package com.example.salonflow.dto.shift;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ShiftResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long branchId;
    private String branchName;
    private Long templateId;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String note;
}
