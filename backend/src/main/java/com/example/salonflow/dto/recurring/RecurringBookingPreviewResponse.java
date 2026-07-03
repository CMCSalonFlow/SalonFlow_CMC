package com.example.salonflow.dto.recurring;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Kết quả trả về từ API preview.
 * FE render calendar dựa trên occurrences, đếm số ngày OK/conflict
 * để hiển thị tóm tắt cho user trước khi confirm.
 */
@Data
@Builder
public class RecurringBookingPreviewResponse {

    private List<OccurrencePreview> occurrences;

    private int totalOccurrences;
    private int conflictCount;
    private int okCount;
}
