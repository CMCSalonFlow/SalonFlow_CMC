package com.example.salonflow.dto.recurring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Request để CONFIRM (tạo thật) sau khi user đã xem preview
 * và quyết định xử lý từng ngày bị conflict.
 *
 * Luồng:
 *   1. FE gọi POST /preview → nhận về danh sách occurrences
 *   2. User xem calendar, với ngày conflict thì chọn:
 *      - SKIP (bỏ qua ngày đó)
 *      - hoặc đổi sang slot khác (newStartTime/newStaffId)
 *   3. FE gọi POST /confirm với danh sách occurrences đã quyết định
 */
@Data
public class RecurringBookingConfirmRequest {

    @Valid
    private RecurringBookingRequest pattern;

    @NotEmpty(message = "Danh sách ngày đặt không được để trống")
    @Valid
    private List<OccurrenceDecision> occurrences;
}
