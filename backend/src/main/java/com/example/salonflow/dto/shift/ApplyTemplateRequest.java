package com.example.salonflow.dto.shift;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request áp dụng template vào 1 tuần cụ thể.
 * weekStartDate: ngày Thứ 2 của tuần muốn áp dụng.
 *
 * Ví dụ:
 *   - "Áp dụng tuần này"  → weekStartDate = ngày Thứ 2 tuần hiện tại
 *   - "Áp dụng tuần sau"  → weekStartDate = ngày Thứ 2 tuần sau
 */
@Data
public class ApplyTemplateRequest {

    @NotNull(message = "Ngày bắt đầu tuần không được để trống")
    private LocalDate weekStartDate;

    /**
     * Nếu true: ghi đè shift đã có trong tuần đó.
     * Nếu false: bỏ qua ngày đã có shift (mặc định).
     */
    private boolean overwrite = false;
}
