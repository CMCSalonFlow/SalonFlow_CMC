package com.example.salonflow.services.impl;

import com.example.salonflow.dto.offday.StaffOffDayRequest;
import com.example.salonflow.dto.offday.StaffOffDayResponse;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Staff;
import com.example.salonflow.entity.StaffOffDay;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.StaffOffDayRepository;
import com.example.salonflow.repository.StaffRepository;
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
    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;

    @Override
    public StaffOffDayResponse createOffDay(Long staffId, StaffOffDayRequest request) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + staffId));

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

        // Tự động hủy booking
        cancelConflictingBookings(staffId, request.getDateFrom(), request.getDateTo());

        return convertToResponse(saved);
    }

    private void cancelConflictingBookings(Long staffId, LocalDate fromDate, LocalDate toDate) {
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookingsWithOffDay(
                staffId, fromDate, toDate);

        for (Booking booking : conflictingBookings) {
            if (booking.getStatus() != BookingStatus.CANCELLED) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setNotes("Đã bị hủy tự động vì nhân viên nghỉ từ " 
                        + fromDate + " đến " + toDate);
            }
        }

        if (!conflictingBookings.isEmpty()) {
            bookingRepository.saveAll(conflictingBookings);
        }
    }

    @Override
    public StaffOffDayResponse updateOffDay(Long offDayId, StaffOffDayRequest request) {
        StaffOffDay offDay = offDayRepository.findById(offDayId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nghỉ"));

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

    private StaffOffDayResponse convertToResponse(StaffOffDay offDay) {
        Staff staff = offDay.getStaff();

        return StaffOffDayResponse.builder()
                .id(offDay.getId())
                .staffId(staff.getId())
                .staffName(staff.getName())
                .dateFrom(offDay.getDateFrom())
                .dateTo(offDay.getDateTo())
                .reason(offDay.getReason())
                .createdBy(offDay.getCreatedBy())
                .createdAt(offDay.getCreatedAt() != null ? offDay.getCreatedAt().toString() : null)
                .build();
    }
}