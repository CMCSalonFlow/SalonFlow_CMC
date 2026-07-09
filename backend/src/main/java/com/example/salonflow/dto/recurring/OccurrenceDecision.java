package com.example.salonflow.dto.recurring;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Quyết định của user cho 1 ngày cụ thể trong chuỗi recurring.
 *
 * - INCLUDE: tạo booking bình thường cho ngày này
 * - SKIP: bỏ qua, không tạo booking ngày này (thường dùng khi conflict)
 *
 * Nếu user muốn đổi giờ cho ngày bị conflict thay vì skip,
 * gửi action=INCLUDE kèm overrideStartTime/overrideEndTime mới.
 */
@Data
public class OccurrenceDecision {

    @NotNull(message = "Ngày không được để trống")
    private LocalDate date;

    @NotNull(message = "Action không được để trống")
    private OccurrenceAction action;

    /** Giờ ghi đè nếu user chọn đổi slot khác cho ngày này (optional) */
    private LocalTime overrideStartTime;

    private LocalTime overrideEndTime;

    public enum OccurrenceAction {
        INCLUDE,
        SKIP
    }
}
