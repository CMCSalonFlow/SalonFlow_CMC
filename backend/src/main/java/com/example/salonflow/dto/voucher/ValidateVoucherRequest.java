package com.example.salonflow.dto.voucher;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateVoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    private String code;
}
