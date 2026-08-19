package com.example.salonflow.services.service;

import com.example.salonflow.dto.offday.CreateSystemOffDayRequest;
import com.example.salonflow.dto.offday.SystemOffDayResponse;

import java.time.LocalDate;
import java.util.List;

public interface SystemOffDayService {
    List<SystemOffDayResponse> getSystemOffDays();

    SystemOffDayResponse createSystemOffDay(CreateSystemOffDayRequest request);

    void deleteSystemOffDay(Long id);

    boolean isBranchClosedOnDate(Long branchId, LocalDate date);

    List<SystemOffDayResponse> getOffDaysForBranchAndRange(Long branchId, LocalDate startDate, LocalDate endDate);
}
