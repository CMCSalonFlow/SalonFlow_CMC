package com.example.salonflow.dto.offday;

import com.example.salonflow.entity.LeaveStatus;
import com.example.salonflow.entity.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffLeaveResponse {

    private Long id;

    private Long staffId;
    private Long staffUserId;
    private String staffName;
    private String staffAvatar;
    private String staffRole;

    private Long branchId;
    private String branchName;

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private long totalDays;

    private LeaveType leaveType;
    private LeaveStatus status;

    private String reason;
    private String rejectionReason;

    private Long approvedBy;
    private String approvedByName;
    private Instant approvedAt;

    private Instant createdAt;
}
