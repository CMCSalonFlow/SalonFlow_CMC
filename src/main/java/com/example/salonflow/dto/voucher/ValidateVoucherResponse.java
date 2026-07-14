package com.example.salonflow.dto.voucher;

import com.example.salonflow.entity.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ValidateVoucherResponse {

    private boolean valid;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;

    /** Số tiền thực tế được giảm (tính từ orderTotal nếu truyền vào) */
    private BigDecimal discountAmount;

    private String message;
}
