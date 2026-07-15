package com.example.salonflow.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DurationMultipleOf15Validator
        implements ConstraintValidator<DurationMultipleOf15, Integer> {

    private static final int UNIT_MINUTES = 15;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {

        // @NotNull xử lý riêng — validator này chỉ check tính hợp lệ của giá trị
        if (value == null) {
            return true;
        }

        return value > 0 && value % UNIT_MINUTES == 0;
    }
}