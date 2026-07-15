package com.example.salonflow.services.impl;

import com.example.salonflow.dto.recurring.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.RecurringBookingStatus;
import com.example.salonflow.entity.enums.RecurringPattern;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.RecurringBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RecurringBookingServiceImpl
 *
 * Logic chính:
 *   1. generateOccurrenceDates() — tính danh sách ngày theo pattern
 *      (WEEKLY: +7 ngày/lần, BIWEEKLY: +14 ngày/lần), giới hạn tối
 *      đa MAX_OCCURRENCES lần để tránh tạo quá nhiều booking.
 *   2. preview() — generate ngày, check conflict cho từng ngày,
 *      KHÔNG ghi DB, chỉ trả về để FE hiển thị calendar.
 *   3. confirm() — nhận quyết định của user cho từng ngày
 *      (INCLUDE/SKIP), tạo RecurringBooking + các Booking con.
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class RecurringBookingServiceImpl implements RecurringBookingService {

    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;

    /**
     * Giới hạn tối đa số lần lặp — tránh user chọn end_date quá xa
     * tạo ra hàng nghìn booking cùng lúc.
     * 26 lần ~ 6 tháng nếu WEEKLY, ~ 1 năm nếu BIWEEKLY.
     */
    private static final int MAX_OCCURRENCES = 26;

    // ════════════════════════════════════════════════════════════
    // PREVIEW
    // ════════════════════════════════════════════════════════════

    @Override
    public RecurringBookingPreviewResponse preview(
            Long customerId,
            RecurringBookingRequest request
    ) {
        validateRequest(request);

        Long staffId = request.getStaffId();
        List<LocalDate> dates = generateOccurrenceDates(
                request.getStartDate(),
                request.getEndDate(),
                parsePattern(request.getPattern())
        );

        List<OccurrencePreview> occurrences = dates.stream()
                .map(date -> {
                    boolean conflict = bookingRepository.existsConflict(
                            staffId,
                            date,
                            request.getStartTime(),
                            request.getEndTime()
                    );

                    return OccurrencePreview.builder()
                            .date(date)
                            .startTime(request.getStartTime())
                            .endTime(request.getEndTime())
                            .hasConflict(conflict)
                            .conflictReason(conflict
                                    ? "Nhân viên đã có lịch khác trùng giờ vào ngày này"
                                     : null)
                            .build();
                })
                .toList();

        int conflictCount = (int) occurrences.stream()
                .filter(OccurrencePreview::isHasConflict)
                .count();

        return RecurringBookingPreviewResponse.builder()
                .occurrences(occurrences)
                .totalOccurrences(occurrences.size())
                .conflictCount(conflictCount)
                .okCount(occurrences.size() - conflictCount)
                .build();
    }

    // ════════════════════════════════════════════════════════════
    // CONFIRM
    // ════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public RecurringBookingResponse confirm(
            Long customerId,
            RecurringBookingConfirmRequest confirmRequest
    ) {
        RecurringBookingRequest request = confirmRequest.getPattern();
        validateRequest(request);

        User customer = findUser(customerId);
        SalonService service = findService(request.getServiceId());
        Staff staff = findStaff(request.getStaffId());
        Branch branch = findBranch(request.getBranchId());

        // Tạo "công thức" recurring booking
        RecurringBooking recurringBooking = RecurringBooking.builder()
                .customer(customer)
                .service(service)
                .staff(staff)
                .branch(branch)
                .pattern(parsePattern(request.getPattern()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(RecurringBookingStatus.ACTIVE)
                .note(request.getNote())
                .build();

        recurringBooking = recurringBookingRepository.save(recurringBooking);

        List<Long> createdBookingIds = new ArrayList<>();
        List<LocalDate> skippedDates = new ArrayList<>();

        // Map ngày → quyết định của user để tra cứu nhanh
        Map<LocalDate, OccurrenceDecision> decisionByDate =
                confirmRequest.getOccurrences().stream()
                        .collect(Collectors.toMap(
                                OccurrenceDecision::getDate,
                                d -> d,
                                (a, b) -> a // nếu trùng ngày, giữ cái đầu
                        ));

        for (OccurrenceDecision decision : confirmRequest.getOccurrences()) {

            if (decision.getAction() == OccurrenceDecision.OccurrenceAction.SKIP) {
                skippedDates.add(decision.getDate());
                continue;
            }

            // Dùng giờ override nếu user đã đổi, không thì dùng giờ pattern chung
            LocalTime startTime = decision.getOverrideStartTime() != null
                    ? decision.getOverrideStartTime()
                    : request.getStartTime();
            LocalTime endTime = decision.getOverrideEndTime() != null
                    ? decision.getOverrideEndTime()
                    : request.getEndTime();

            // Check lại conflict tại thời điểm confirm (phòng race condition
            // giữa lúc preview và lúc confirm có booking khác chen vào)
            boolean stillConflict = bookingRepository.existsConflict(
                    staff.getId(), decision.getDate(), startTime, endTime
            );

            if (stillConflict) {
                log.warn("[RecurringBooking] Bỏ qua ngày {} do vẫn còn conflict tại thời điểm confirm",
                        decision.getDate());
                skippedDates.add(decision.getDate());
                continue;
            }

            String slotKey = SlotLockServiceImpl.buildSlotKey(
                    branch.getId(),
                    staff.getId(),
                    decision.getDate().toString(),
                    startTime.toString()
            );

            Booking booking = Booking.builder()
                    .customer(customer)
                    .branch(branch)
                    .recurringBooking(recurringBooking)
                    .bookingDate(decision.getDate())
                    .startTime(startTime)
                    .endTime(endTime)
                    .slotKey(slotKey)
                    .status(BookingStatus.PENDING)
                    .totalPrice(service.getPrice())
                    .totalDurationMinutes(service.getDurationMinutes())
                    .notes(request.getNote())
                    .assignedStaff(staff)
                    .build();

            // Khởi tạo BookingItem và liên kết
            BookingItem item = BookingItem.builder()
                    .booking(booking)
                    .service(service)
                    .price(service.getPrice())
                    .durationMinutes(service.getDurationMinutes())
                    .build();
            booking.getItems().add(item);

            booking = bookingRepository.save(booking);
            createdBookingIds.add(booking.getId());
        }

        log.info("[RecurringBooking] Tạo xong: recurringId={} created={} skipped={}",
                recurringBooking.getId(), createdBookingIds.size(), skippedDates.size());

        return RecurringBookingResponse.builder()
                .id(recurringBooking.getId())
                .customerId(customer.getId())
                .serviceId(service.getId())
                .serviceName(service.getName())
                .staffId(staff.getId())
                .staffName(staff.getName())
                .branchId(branch.getId())
                .branchName(branch.getName())
                .pattern(recurringBooking.getPattern().name())
                .startDate(recurringBooking.getStartDate())
                .endDate(recurringBooking.getEndDate())
                .status(recurringBooking.getStatus().name())
                .createdBookingIds(createdBookingIds)
                .skippedDates(skippedDates)
                .build();
    }

    // ════════════════════════════════════════════════════════════
    // CANCEL
    // ════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void cancelRecurring(Long customerId, Long recurringBookingId) {

        RecurringBooking recurringBooking = recurringBookingRepository
                .findById(recurringBookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recurring booking not found: " + recurringBookingId));

        if (!recurringBooking.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hủy lịch định kỳ này."
            );
        }

        recurringBooking.setStatus(RecurringBookingStatus.CANCELLED);
        recurringBookingRepository.save(recurringBooking);

        // Hủy các booking con chưa diễn ra (chỉ hủy PENDING/CONFIRMED)
        List<Booking> bookings = bookingRepository
                .findByRecurringBookingId(recurringBookingId);

        LocalDate today = LocalDate.now();
        for (Booking booking : bookings) {
            boolean isFuture = !booking.getBookingDate().isBefore(today);
            boolean isCancellable = booking.getStatus() == BookingStatus.PENDING
                    || booking.getStatus() == BookingStatus.CONFIRMED;

            if (isFuture && isCancellable) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            }
        }

        log.info("[RecurringBooking] Đã hủy chuỗi: id={}", recurringBookingId);
    }

    // ════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════

    /**
     * Generate danh sách ngày lặp theo pattern.
     * WEEKLY: start, start+7, start+14, ...
     * BIWEEKLY: start, start+14, start+28, ...
     *
     * Dừng khi vượt quá endDate HOẶC đạt MAX_OCCURRENCES.
     */
    private List<LocalDate> generateOccurrenceDates(
            LocalDate startDate,
            LocalDate endDate,
            RecurringPattern pattern
    ) {
        int stepDays = (pattern == RecurringPattern.BIWEEKLY) ? 14 : 7;

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate) && dates.size() < MAX_OCCURRENCES) {
            dates.add(current);
            current = current.plusDays(stepDays);
        }

        return dates;
    }

    private void validateRequest(RecurringBookingRequest request) {

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException(
                    "Ngày kết thúc phải sau ngày bắt đầu");
        }

        if (request.getEndTime().isBefore(request.getStartTime())
                || request.getEndTime().equals(request.getStartTime())) {
            throw new IllegalArgumentException(
                    "Giờ kết thúc phải sau giờ bắt đầu");
        }

        RecurringPattern pattern = parsePattern(request.getPattern());
        int stepDays = (pattern == RecurringPattern.BIWEEKLY) ? 14 : 7;

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                request.getStartDate(), request.getEndDate());
        long estimatedOccurrences = (totalDays / stepDays) + 1;

        if (estimatedOccurrences > MAX_OCCURRENCES) {
            throw new IllegalArgumentException(
                    "Khoảng thời gian quá dài. Tối đa " + MAX_OCCURRENCES
                            + " lần lặp (~ "
                            + (MAX_OCCURRENCES * stepDays / 7) + " tuần). "
                            + "Vui lòng chọn ngày kết thúc gần hơn.");
        }
    }

    private RecurringPattern parsePattern(String pattern) {
        try {
            return RecurringPattern.valueOf(pattern.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Pattern không hợp lệ. Chỉ chấp nhận WEEKLY hoặc BIWEEKLY");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));
    }

    private SalonService findService(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found: " + serviceId));
    }

    private Staff findStaff(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff not found: " + staffId));
    }

    private Branch findBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found: " + branchId));
    }
}
