package com.example.salonflow.dto.support;

import com.example.salonflow.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTicketStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private TicketStatus status;

    private String note;
}
