package com.example.salonflow.dto.booking;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class CheckInBookingResponse {

    private boolean success;
    private String message;
    private Long bookingId;
    private String status;
    private LocalDateTime checkedInAt;
    private String customerName;
    private String customerPhone;
    private Long branchId;
    private String branchName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long assignedStaffId;
    private String assignedStaffName;
}
