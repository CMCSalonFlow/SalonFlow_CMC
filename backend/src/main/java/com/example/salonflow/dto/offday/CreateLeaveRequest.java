package com.example.salonflow.dto.offday;

import com.example.salonflow.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateLeaveRequest {

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate dateFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate dateTo;

    private LeaveType leaveType;

    private String reason;
}
