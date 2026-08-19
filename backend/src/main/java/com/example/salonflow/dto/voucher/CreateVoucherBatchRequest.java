package com.example.salonflow.dto.voucher;

import com.example.salonflow.entity.enums.DiscountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherBatchRequest {
    @NotBlank(message = "Prefix không được để trống")
    private String prefix;

    @NotNull(message = "Số lượng không được để trống")
    @Min(1)
    @Max(500)
    private Integer quantity = 10;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    private BigDecimal discountValue;

    private Integer maxUses = 1;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private LocalDateTime expiresAt;
}
