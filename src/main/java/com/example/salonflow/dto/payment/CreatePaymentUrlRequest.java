package com.example.salonflow.dto.payment;

import com.example.salonflow.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentUrlRequest {

    @NotNull(message = "Booking ID khong duoc de trong")
    private Long bookingId;

    @NotNull(message = "Phuong thuc thanh toan khong duoc de trong")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Idempotency Key khong duoc de trong")
    private String idempotencyKey;

    @NotBlank(message = "Return URL khong duoc de trong")
    private String returnUrl;
}
