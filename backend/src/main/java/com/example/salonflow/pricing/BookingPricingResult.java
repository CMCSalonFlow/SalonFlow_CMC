package com.example.salonflow.pricing;

import com.example.salonflow.entity.SalonService;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class BookingPricingResult {

    private BigDecimal totalPrice;

    private BigDecimal depositAmount;

    private BigDecimal remainingAmount;

    private Integer totalDurationMinutes;

    private List<SalonService> services;

}