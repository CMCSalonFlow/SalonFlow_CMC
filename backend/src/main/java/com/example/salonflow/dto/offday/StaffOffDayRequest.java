package com.example.salonflow.dto.offday;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffOffDayRequest {

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate dateFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate dateTo;

    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;

    // Validation: dateTo phải >= dateFrom
    @AssertTrue(message = "Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu")
    private boolean isValidDateRange() {
        if (dateFrom == null || dateTo == null) {
            return true; // Sẽ bị @NotNull bắt trước
        }
        return !dateTo.isBefore(dateFrom);
    }
}