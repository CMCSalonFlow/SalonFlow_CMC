package com.example.salonflow.dto.support;

import com.example.salonflow.entity.enums.TicketCategory;
import com.example.salonflow.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTicketRequest {

    @NotBlank(message = "Tiêu đề ticket không được để trống")
    private String subject;

    @NotBlank(message = "Mô tả chi tiết không được để trống")
    private String description;

    @NotNull(message = "Danh mục yêu cầu không được để trống")
    private TicketCategory category;

    @NotNull(message = "Mức độ ưu tiên (P1, P2, P3) không được để trống")
    private TicketPriority priority;
}
