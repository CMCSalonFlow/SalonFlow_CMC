package com.example.salonflow.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validate giá trị (phút) phải là bội số của 15 và > 0.
 *
 * Lý do: duration_minutes ảnh hưởng trực tiếp đến cách hệ thống
 * chia slot booking (15 phút / slot). Giá trị lệch (ví dụ 20 phút)
 * sẽ khiến slot không khớp lịch của các dịch vụ khác trong cùng
 * branch/staff.
 *
 * Dùng trên field Integer/int trong DTO request:
 *
 *   @DurationMultipleOf15
 *   private Integer durationMinutes;
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DurationMultipleOf15Validator.class)
@Documented
public @interface DurationMultipleOf15 {

    String message() default "Thời gian thực hiện phải là bội số của 15 phút (vd: 15, 30, 45, 60...)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}