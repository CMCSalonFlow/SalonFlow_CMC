package com.example.salonflow.dto.recurring;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RecurringBookingResponse {

    private Long id;
    private Long customerId;
    private Long serviceId;
    private String serviceName;
    private Long staffId;
    private String staffName;
    private Long branchId;
    private String branchName;
    private String pattern;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    /** Danh sách booking đã được tạo thành công */
    private List<Long> createdBookingIds;

    /** Danh sách ngày bị bỏ qua (do user chọn SKIP) */
    private List<LocalDate> skippedDates;
}
