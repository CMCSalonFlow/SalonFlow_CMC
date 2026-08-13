package com.example.salonflow.dto.subscription;

import com.example.salonflow.entity.enums.BillingCycle;
import com.example.salonflow.entity.enums.SubscriptionPlan;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualSubscriptionRequest {

    @NotNull(message = "Salon ID cannot be null")
    private Long salonId;

    @NotNull(message = "Plan cannot be null")
    private SubscriptionPlan plan;

    @NotNull(message = "Billing cycle cannot be null")
    private BillingCycle billingCycle;

    @NotNull(message = "Price cannot be null")
    @Min(value = 0, message = "Price must be at least 0")
    private BigDecimal price;

    @Min(value = 1, message = "Duration must be at least 1 day")
    @NotNull(message = "Duration cannot be null")
    private Integer durationDays;
}
