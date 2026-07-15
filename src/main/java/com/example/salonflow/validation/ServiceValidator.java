package com.example.salonflow.validation;

import com.example.salonflow.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ServiceValidator {
public void validateDeposit(
        Boolean depositRequired,
        BigDecimal depositPercentage
) {

    boolean required = Boolean.TRUE.equals(depositRequired);

    if (required) {

        if (depositPercentage == null) {
            throw new BadRequestException(
                    "Deposit percentage is required when deposit is enabled."
            );
        }

        if (depositPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Deposit percentage must be greater than 0."
            );
        }

        if (depositPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException(
                    "Deposit percentage cannot exceed 100."
            );
        }
    }
}

}