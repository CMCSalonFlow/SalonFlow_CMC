package com.example.salonflow.dto.offday;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffOffDayResponse {

    private Long id;
    private Long staffId;
    private String staffName;        // Tên nhân viên để hiển thị
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String reason;
    private String createdBy;
    private String createdAt;        // Có thể format đẹp hơn sau
}