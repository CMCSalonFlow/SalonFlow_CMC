package com.example.salonflow.dto.bundle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBundleRequest {

    @NotBlank(message = "Tên combo/gói dịch vụ không được để trống")
    @Size(max = 255, message = "Tên combo/gói dịch vụ tối đa 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá combo không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá combo không được âm")
    private BigDecimal price;

    private Boolean isActive;

    @NotEmpty(message = "Danh sách dịch vụ không được để trống")
    @Size(min = 2, message = "Combo phải chứa ít nhất 2 dịch vụ")
    @Valid
    private List<BundleItemRequest> items;
}
