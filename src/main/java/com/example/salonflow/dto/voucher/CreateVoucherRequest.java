package com.example.salonflow.dto.voucher;

import com.example.salonflow.entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherRequest {

    @NotBlank(message = "Code không được để trống")
    @Size(min = 4, max = 50, message = "Code phải từ 4-50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code chỉ chứa chữ hoa, số, gạch dưới, gạch ngang")
    private String code;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    @NotNull(message = "Số lần dùng tối đa không được để trống")
    @Min(value = 1, message = "Số lần dùng tối đa phải ít nhất là 1")
    private Integer maxUses;

    @NotNull(message = "Ngày hết hạn không được để trống")
    @Future(message = "Ngày hết hạn phải ở tương lai")
    private LocalDateTime expiresAt;
}
