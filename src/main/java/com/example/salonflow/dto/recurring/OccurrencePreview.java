package com.example.salonflow.dto.recurring;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 1 ngày trong kết quả preview.
 * FE dùng để render calendar — ngày OK hiển thị bình thường,
 * ngày CONFLICT tô đỏ kèm lý do.
 */
@Data
@Builder
public class OccurrencePreview {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    /** true nếu slot này đã có booking khác trùng giờ */
    private boolean hasConflict;

    /** Lý do conflict (null nếu hasConflict = false) */
    private String conflictReason;
}
