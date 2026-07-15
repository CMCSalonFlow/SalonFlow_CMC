package com.example.salonflow.dto.voucher;

import com.example.salonflow.entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tạo batch voucher: sinh ra `quantity` voucher với prefix + random suffix.
 * Ví dụ: prefix="SALE", quantity=10 → SALE_A3F9K, SALE_B2X1P, ...
 */
@Data
public class BatchCreateVoucherRequest {

    @NotBlank(message = "Prefix không được để trống")
    @Size(max = 20, message = "Prefix tối đa 20 ký tự")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Prefix chỉ chứa chữ hoa và số")
    private String prefix;

    @NotNull
    @Min(value = 1, message = "Số lượng ít nhất là 1")
    @Max(value = 500, message = "Số lượng tối đa là 500")
    private Integer quantity;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    // Mỗi code chỉ dùng được 1 lần (AC: 1 lần/user, maxUses = 1)
    // Có thể mở rộng nếu muốn

    @NotNull
    @Future(message = "Ngày hết hạn phải ở tương lai")
    private LocalDateTime expiresAt;
}
