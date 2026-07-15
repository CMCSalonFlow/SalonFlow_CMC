package com.example.salonflow.dto.booking;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateWalkInBookingRequest {

    private String customerName;

    private String customerPhone;

    private Long preferredStaffId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private List<Long> serviceIds;

    private Long bundleId;

    private String notes;
}