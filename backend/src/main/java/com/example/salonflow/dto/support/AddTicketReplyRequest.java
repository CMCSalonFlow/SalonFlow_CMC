package com.example.salonflow.dto.support;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTicketReplyRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String message;

    private Boolean isInternalNote = false;
}
