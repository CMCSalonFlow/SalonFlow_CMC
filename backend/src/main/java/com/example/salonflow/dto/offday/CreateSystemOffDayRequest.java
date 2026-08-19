package com.example.salonflow.dto.offday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSystemOffDayRequest {
    @NotBlank(message = "Tên ngày lễ / dịp nghỉ không được để trống")
    private String title;

    @NotNull(message = "Từ ngày không được để trống")
    private LocalDate dateFrom;

    @NotNull(message = "Đến ngày không được để trống")
    private LocalDate dateTo;

    private Long branchId; // null = Toàn Salon (Tất cả chi nhánh)
    private Boolean isAllBranches;
    private String reason;
}
