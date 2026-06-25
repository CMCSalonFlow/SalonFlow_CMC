package com.example.salonflow.services.impl;

import com.example.salonflow.dto.offday.StaffOffDayRequest;
import com.example.salonflow.dto.offday.StaffOffDayResponse;
import com.example.salonflow.entity.StaffOffDay;
import com.example.salonflow.entity.User;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.StaffOffDayRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.services.service.StaffOffDayService;
import com.example.salonflow.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffOffDayServiceImpl implements StaffOffDayService {

    private final StaffOffDayRepository offDayRepository;
    private final UserRepository userRepository;

    @Override
    public StaffOffDayResponse createOffDay(Long staffId, StaffOffDayRequest request) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + staffId));

        // Kiểm tra trùng lặp khoảng thời gian
        boolean exists = offDayRepository.existsByStaffIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(
                staffId, request.getDateFrom(), request.getDateTo());

        if (exists) {
            throw new BadRequestException("Nhân viên đã có lịch nghỉ trong khoảng thời gian này");
        }

        StaffOffDay offDay = StaffOffDay.builder()
                .staff(staff)
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .reason(request.getReason())
                .createdBy(SecurityUtil.getCurrentUsername())
                .build();

        StaffOffDay saved = offDayRepository.save(offDay);

        // TODO: Khi hoàn thiện module Booking thì mở ra
        // cancelConflictingBookings(staffId, request.getDateFrom(), request.getDateTo());

        return convertToResponse(saved);
    }

    @Override
    public StaffOffDayResponse updateOffDay(Long offDayId, StaffOffDayRequest request) {
        StaffOffDay offDay = offDayRepository.findById(offDayId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nghỉ"));

        // Kiểm tra overlap với các lịch nghỉ khác (không tính chính nó)
        List<StaffOffDay> overlapping = offDayRepository.findOverlappingOffDays(
                offDay.getStaff().getId(), request.getDateFrom(), request.getDateTo(), offDayId);

        if (!overlapping.isEmpty()) {
            throw new BadRequestException("Khoảng thời gian nghỉ bị trùng với lịch nghỉ khác");
        }

        offDay.setDateFrom(request.getDateFrom());
        offDay.setDateTo(request.getDateTo());
        offDay.setReason(request.getReason());

        StaffOffDay updated = offDayRepository.save(offDay);
        return convertToResponse(updated);
    }

    @Override
    public void deleteOffDay(Long offDayId) {
        StaffOffDay offDay = offDayRepository.findById(offDayId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nghỉ"));
        offDayRepository.delete(offDay);
    }

    @Override
    public List<StaffOffDayResponse> getOffDaysByStaffId(Long staffId) {
        List<StaffOffDay> offDays = offDayRepository.findByStaffIdOrderByDateFromDesc(staffId);
        return offDays.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StaffOffDayResponse> getOffDaysInRange(Long staffId, LocalDate startDate, LocalDate endDate) {
        List<StaffOffDay> offDays = offDayRepository.findOffDaysInRange(staffId, startDate, endDate);
        return offDays.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public boolean isStaffOffInPeriod(Long staffId, LocalDate date) {
        return isStaffOffInPeriod(staffId, date, date);
    }

    @Override
    public boolean isStaffOffInPeriod(Long staffId, LocalDate startDate, LocalDate endDate) {
        return offDayRepository.existsByStaffIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(
                staffId, startDate, endDate);
    }

    /**
     * TODO: Implement sau khi hoàn thiện module Booking
     */
    private void cancelConflictingBookings(Long staffId, LocalDate fromDate, LocalDate toDate) {
        // Sẽ implement khi có BookingRepository
        System.out.println("[TODO] Hủy booking conflict cho staff " + staffId 
                + " từ " + fromDate + " đến " + toDate);
    }

    private StaffOffDayResponse convertToResponse(StaffOffDay offDay) {
        return StaffOffDayResponse.builder()
                .id(offDay.getId())
                .staffId(offDay.getStaff().getId())
                .staffName(offDay.getStaff().getFullName() != null ? 
                          offDay.getStaff().getFullName() : offDay.getStaff().getUsername())
                .dateFrom(offDay.getDateFrom())
                .dateTo(offDay.getDateTo())
                .reason(offDay.getReason())
                .createdBy(offDay.getCreatedBy())
                .createdAt(offDay.getCreatedAt() != null ? offDay.getCreatedAt().toString() : null)
                .build();
    }
}