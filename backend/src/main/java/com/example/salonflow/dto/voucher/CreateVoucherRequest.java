package com.example.salonflow.dto.voucher;

import com.example.salonflow.entity.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    private BigDecimal discountValue;

    private Integer maxUses = 1;

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private LocalDateTime expiresAt;
}
