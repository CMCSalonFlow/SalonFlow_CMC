package com.example.salonflow.dto.subscription;

import com.example.salonflow.entity.enums.BillingCycle;
import com.example.salonflow.entity.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeCheckoutRequest {

    @NotNull(message = "Plan cannot be null")
    private SubscriptionPlan plan;

    @NotNull(message = "Billing cycle cannot be null")
    private BillingCycle billingCycle;

    @NotNull(message = "Success URL cannot be null")
    private String successUrl;

    @NotNull(message = "Cancel URL cannot be null")
    private String cancelUrl;
}
