package com.example.salonflow.dto.cancellation;

import lombok.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCancellationPolicyRequest {

    @Min(value = 1, message = "Số giờ hủy miễn phí tối thiểu là 1 giờ")
    private Integer freeCancelHours;

    @DecimalMin(value = "0.0", message = "Phí hủy phải >= 0")
    @DecimalMax(value = "100.0", message = "Phí hủy tối đa là 100%")
    private BigDecimal feePercentage;

    private Boolean isActive;
}