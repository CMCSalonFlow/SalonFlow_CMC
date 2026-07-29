package com.example.salonflow.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosCashPaymentRequest {

    @NotNull(message = "Mã lịch hẹn không được để trống")
    private Long bookingId;

    @NotNull(message = "Số tiền thu không được để trống")
    @Positive(message = "Số tiền thu phải lớn hơn 0")
    private BigDecimal amount;

    private String notes;
}
