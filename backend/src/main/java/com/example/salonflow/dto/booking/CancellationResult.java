package com.example.salonflow.dto.booking;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationResult {
    private boolean success;
    private BigDecimal feeAmount;
    private BigDecimal refundAmount;
    private String message;
    private boolean isFreeCancel;
}
