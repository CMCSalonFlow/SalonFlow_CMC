package com.example.salonflow.dto.shift;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateShiftTemplateRequest {

    @NotNull(message = "Branch không được để trống")
    private Long branchId;

    @NotNull(message = "Staff không được để trống")
    private Long userId;

    @NotBlank(message = "Tên template không được để trống")
    private String name;

    private String description;

    @Valid
    private List<ShiftTemplateDetailRequest> details;
}
