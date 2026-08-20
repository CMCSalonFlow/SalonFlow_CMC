package com.example.salonflow.services.service;

import com.example.salonflow.dto.offday.*;
import com.example.salonflow.entity.LeaveStatus;

import java.time.LocalDate;
import java.util.List;

public interface StaffOffDayService {

    // APIs cho Nhân viên & Manager tạo / quản lý đơn xin nghỉ của mình
    StaffLeaveResponse createLeaveRequest(Long userId, CreateLeaveRequest request);

    List<StaffLeaveResponse> getMyLeaveRequests(Long userId);

    void cancelLeaveRequest(Long userId, Long leaveId);

    // APIs cho Owner & Manager duyệt đơn nghỉ phép
    List<StaffLeaveResponse> getApprovalList(Long userId, LeaveStatus status, Long branchId);

    StaffLeaveResponse approveLeaveRequest(Long userId, Long leaveId);

    StaffLeaveResponse rejectLeaveRequest(Long userId, Long leaveId, RejectLeaveRequest request);

    // Legacy methods
    StaffOffDayResponse createOffDay(Long staffId, StaffOffDayRequest request);

    StaffOffDayResponse updateOffDay(Long offDayId, StaffOffDayRequest request);

    void deleteOffDay(Long offDayId);

    List<StaffOffDayResponse> getOffDaysByStaffId(Long staffId);

    List<StaffOffDayResponse> getOffDaysInRange(Long staffId, LocalDate startDate, LocalDate endDate);

    boolean isStaffOffInPeriod(Long staffId, LocalDate date);

    boolean isStaffOffInPeriod(Long staffId, LocalDate startDate, LocalDate endDate);

    List<StaffLeaveResponse> getApprovedLeaves(Long branchId, LocalDate startDate, LocalDate endDate);
}