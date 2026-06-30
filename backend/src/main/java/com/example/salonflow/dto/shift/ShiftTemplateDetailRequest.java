package com.example.salonflow.dto.shift;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ShiftTemplateDetailRequest {

    @NotNull(message = "Ngày trong tuần không được để trống")
    @Min(value = 1, message = "Ngày trong tuần từ 1 (Thứ 2) đến 7 (CN)")
    @Max(value = 7, message = "Ngày trong tuần từ 1 (Thứ 2) đến 7 (CN)")
    private Integer dayOfWeek;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;
}
