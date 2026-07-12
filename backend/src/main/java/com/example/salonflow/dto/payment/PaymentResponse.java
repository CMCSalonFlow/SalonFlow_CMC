package com.example.salonflow.dto.payment;

import com.example.salonflow.entity.enums.PaymentMethod;
import com.example.salonflow.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long bookingId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentUrl;
}
