package com.example.salonflow.dto.support;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignTicketRequest {

    @NotNull(message = "ID cán bộ xử lý không được để trống")
    private Long assigneeUserId;
}
