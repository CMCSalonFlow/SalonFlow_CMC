package com.example.salonflow.dto.service;

import com.example.salonflow.validation.DurationMultipleOf15;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 255, message = "Tên dịch vụ tối đa 255 ký tự")
    private String name;

    private Long categoryId;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá không được âm")
    private BigDecimal price;

    @NotNull(message = "Thời gian thực hiện không được để trống")
    @DurationMultipleOf15
    private Integer durationMinutes;

    private String description;

    private Boolean depositRequired;

    private BigDecimal depositPercentage;
    /** Danh sách URL ảnh (đã upload sẵn lên MinIO trước đó) */
    private List<String> images;
}