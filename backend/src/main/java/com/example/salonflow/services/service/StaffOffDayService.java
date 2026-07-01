package com.example.salonflow.services.service;

import com.example.salonflow.dto.offday.StaffOffDayRequest;
import com.example.salonflow.dto.offday.StaffOffDayResponse;
import com.example.salonflow.entity.StaffOffDay;

import java.time.LocalDate;
import java.util.List;

public interface StaffOffDayService {

    StaffOffDayResponse createOffDay(Long staffId, StaffOffDayRequest request);

    StaffOffDayResponse updateOffDay(Long offDayId, StaffOffDayRequest request);

    void deleteOffDay(Long offDayId);

    List<StaffOffDayResponse> getOffDaysByStaffId(Long staffId);

    List<StaffOffDayResponse> getOffDaysInRange(Long staffId, LocalDate startDate, LocalDate endDate);

    boolean isStaffOffInPeriod(Long staffId, LocalDate date);
    
    boolean isStaffOffInPeriod(Long staffId, LocalDate startDate, LocalDate endDate);
}