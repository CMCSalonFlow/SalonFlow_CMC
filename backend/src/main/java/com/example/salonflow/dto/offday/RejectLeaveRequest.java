package com.example.salonflow.dto.offday;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectLeaveRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectionReason;
}
