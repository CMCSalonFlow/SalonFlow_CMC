package com.example.salonflow.dto.loyalty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RedeemPointsRequest {
    @NotNull(message = "Points to redeem is required")
    @Min(value = 100, message = "Minimum 100 points required to redeem")
    private Integer pointsToRedeem;
}
