package com.example.salonflow.ai.dto.scheduling;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSmartSchedulingConfigDto {

    @NotNull(message = "Trọng số tải công việc không được để trống")
    @DecimalMin(value = "0.0", message = "Trọng số phải nằm trong khoảng 0.0 - 1.0")
    @DecimalMax(value = "1.0", message = "Trọng số phải nằm trong khoảng 0.0 - 1.0")
    private BigDecimal workloadWeight;

    @NotNull(message = "Trọng số di chuyển/khoảng nghỉ không được để trống")
    @DecimalMin(value = "0.0", message = "Trọng số phải nằm trong khoảng 0.0 - 1.0")
    @DecimalMax(value = "1.0", message = "Trọng số phải nằm trong khoảng 0.0 - 1.0")
    private BigDecimal travelWeight;

    @NotNull(message = "Trọng số kỹ năng/dịch vụ không được để trống")
    @DecimalMin(value = "0.0", message = "Trọng số phải nằm trong khoảng 0.0 - 1.0")
    @DecimalMax(value = "1.0", message = "Trọng số phải nằm trong khoảng 0.0 - 1.0")
    private BigDecimal serviceFitWeight;

    private String description;
}
