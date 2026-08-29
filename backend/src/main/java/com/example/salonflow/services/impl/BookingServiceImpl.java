package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.booking.CreateGuestBookingRequest;
import com.example.salonflow.dto.booking.CreateBookingRequest;
import com.example.salonflow.dto.booking.CreateWalkInBookingRequest;
import com.example.salonflow.dto.booking.BookingResponse;
import com.example.salonflow.dto.booking.BookingItemResponse;
import com.example.salonflow.dto.booking.CheckInBookingResponse;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.PaymentStatus;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.InvalidTokenException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.notification.email.BookingQrSignatureService;
import com.example.salonflow.pricing.BookingPricingResult;
import com.example.salonflow.pricing.BookingPricingService;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.BookingService;
import com.example.salonflow.services.service.PaymentService;
import com.example.salonflow.dto.payment.PaymentResponse;
import com.example.salonflow.util.SecurityUtil;
import com.example.salonflow.notification.BookingNotificationEvent;
import com.example.salonflow.notification.BookingNotificationType;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.entity.enums.ShiftStatus;
import com.example.salonflow.websocket.BookingWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Lớp triển khai các nghiệp vụ liên quan đến Đặt lịch hẹn (BookingService).
 * Sử dụng tên đầy đủ của chú thích @Service để tránh xung đột với thực thể Service của dự án.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceBundleRepository serviceBundleRepository;
    private final BranchHourRepository branchHourRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository; // ← Thêm
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ShiftRepository shiftRepository;
    private final StaffOffDayRepository staffOffDayRepository;
    private final BookingWebSocketHandler bookingWebSocketHandler;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BookingQrSignatureService bookingQrSignatureService;

    private final SmartSchedulingLogRepository smartSchedulingLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final CustomerProfileRepository customerProfileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingPricingService bookingPricingService;
    private final com.example.salonflow.services.service.LoyaltyPointService loyaltyPointService;
    private final com.example.salonflow.ai.service.NoShowPredictionService noShowPredictionService;
    private final NoShowPredictionRepository predictionRepository;
    private final ReviewRepository reviewRepository;
    private final com.example.salonflow.services.service.InvoicePdfService invoicePdfService;
    private final com.example.salonflow.services.service.EmailService emailService;
    private final com.example.salonflow.services.service.SystemOffDayService systemOffDayService;

    @Override
    @Transactional
    public BookingResponse create(Long branchId, CreateBookingRequest request) {
        User customer;
        if (request.getCustomerId() != null) {
            customer = userRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy khách hàng với id: " + request.getCustomerId()));
        } else {
            customer = getCurrentUser();
        }

        if (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank()) {
            String phone = request.getCustomerPhone().trim();
            if (customer.getPhone() == null || customer.getPhone().isBlank() || !customer.getPhone().equals(phone)) {
                customer.setPhone(phone);
                customer = userRepository.save(customer);
            }
        }

        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            String name = request.getCustomerName().trim();
            if (customer.getFullName() == null || customer.getFullName().isBlank()) {
                customer.setFullName(name);
                customer = userRepository.save(customer);
            }
        }

        return createBookingInternal(
                branchId,
                customer,
                request.getBookingDate(),
                request.getStartTime(),
                request.getPreferredStaffId(),
                request.getServiceIds(),
                request.getBundleId(),
                request.getNotes()
        );
    }

    @Override
    @Transactional
    public BookingResponse createGuestBooking(Long branchId, CreateGuestBookingRequest request) {
        User customer = findOrCreateGuestCustomer(request);
        return createBookingInternal(
                branchId,
                customer,
                request.getBookingDate(),
                request.getStartTime(),
                request.getPreferredStaffId(),
                request.getServiceIds(),
                request.getBundleId(),
                request.getNotes()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getByBranch(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId);
        }
        return toResponses(bookingRepository.findByBranchId(branchId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getByCustomerId(Long customerId) {
        return toResponses(bookingRepository.findByCustomerId(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> searchBookings(Long branchId, BookingStatus status, LocalDate fromDate, LocalDate toDate, String search, Pageable pageable) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId);
        }
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Booking> page = bookingRepository.searchBookings(branchId, status, fromDate, toDate, cleanSearch, pageable);
        List<BookingResponse> responses = toResponses(page.getContent());
        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    private List<BookingResponse> toResponses(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return List.of();
        }

        List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();

        // 1. Batch fetch reviews for all bookings in 1 query
        Set<Long> reviewedBookingIds = reviewRepository.findByBookingIdIn(bookingIds).stream()
                .map(r -> r.getBooking().getId())
                .collect(Collectors.toSet());

        // 2. Batch fetch prediction logs for all bookings in 1 query
        Map<Long, NoShowPredictionLog> predictionLogMap = predictionRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(NoShowPredictionLog::getBookingId, p -> p, (a, b) -> a));

        return bookings.stream().map(booking -> {
            List<BookingItemResponse> itemResponses = booking.getItems().stream()
                    .map(item -> BookingItemResponse.builder()
                            .id(item.getId())
                            .serviceId(item.getService() != null ? item.getService().getId() : null)
                            .serviceName(item.getService() != null ? item.getService().getName() : null)
                            .bundleId(item.getBundle() != null ? item.getBundle().getId() : null)
                            .bundleName(item.getBundle() != null ? item.getBundle().getName() : null)
                            .price(item.getPrice())
                            .durationMinutes(item.getDurationMinutes())
                            .build())
                    .toList();

            NoShowPredictionLog predLog = predictionLogMap.get(booking.getId());
            com.example.salonflow.ai.dto.noshow.NoShowPredictionDto predictionDto = null;
            if (predLog != null) {
                double prob = predLog.getProbability() != null ? predLog.getProbability() : 0.0;
                predictionDto = com.example.salonflow.ai.dto.noshow.NoShowPredictionDto.builder()
                        .logId(predLog.getId())
                        .bookingId(booking.getId())
                        .riskLevel(predLog.getRiskLevel())
                        .probability(prob)
                        .probabilityPercentage(Math.round(prob * 1000.0) / 10.0)
                        .explanation(predLog.getExplanation())
                        .smsSent(Boolean.TRUE.equals(predLog.getSmsSent()))
                        .build();
            }

            LocalDateTime reviewedAt = booking.getReviewedAt();
            if (reviewedAt == null && reviewedBookingIds.contains(booking.getId())) {
                reviewedAt = LocalDateTime.now();
            }

            String customerPhone = (booking.getCustomer() != null && booking.getCustomer().getPhone() != null && !booking.getCustomer().getPhone().isBlank())
                    ? booking.getCustomer().getPhone()
                    : ((booking.getCustomer() != null && booking.getCustomer().getUsername() != null && booking.getCustomer().getUsername().matches("^0[0-9]{9,10}$")) ? booking.getCustomer().getUsername() : null);

            return BookingResponse.builder()
                    .id(booking.getId())
                    .customerId(booking.getCustomer().getId())
                    .customerName(booking.getCustomer().getFullName())
                    .customerPhone(customerPhone)
                    .branchId(booking.getBranch().getId())
                    .branchName(booking.getBranch().getName())
                    .bookingDate(booking.getBookingDate())
                    .startTime(booking.getStartTime())
                    .endTime(booking.getEndTime())
                    .preferredStaffId(booking.getPreferredStaff() != null ? booking.getPreferredStaff().getId() : null)
                    .preferredStaffName(booking.getPreferredStaff() != null ? booking.getPreferredStaff().getName() : null)
                    .assignedStaffId(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getId() : null)
                    .assignedStaffName(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getName() : null)
                    .status(booking.getStatus().name())
                    .totalPrice(booking.getTotalPrice())
                    .depositAmount(booking.getDepositAmount())
                    .remainingAmount(booking.getRemainingAmount())
                    .totalDurationMinutes(booking.getTotalDurationMinutes())
                    .notes(booking.getNotes())
                    .invoiceUrl(booking.getInvoiceUrl())
                    .reviewedAt(reviewedAt)
                    .items(itemResponses)
                    .noShowPrediction(predictionDto)
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getById(Long branchId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn với id: " + bookingId));
        if (!booking.getBranch().getId().equals(branchId)) {
            throw new BusinessException("Lịch hẹn này không thuộc chi nhánh bạn chỉ định");
        }
        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long branchId, LocalDate date, List<Long> serviceIds, Long bundleId, Long staffId) {
        // 1. Tính toán tổng thời lượng cần đặt
        int totalDuration = 0;
        List<SalonService> services = new ArrayList<>();
        if (bundleId != null) {
            ServiceBundle bundle = serviceBundleRepository.findByIdAndBranchId(bundleId, branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy combo với id: " + bundleId + " tại chi nhánh này"));
            BookingPricingResult pricingResult = bookingPricingService.calculate(branchId, null, bundle);
            totalDuration = pricingResult.getTotalDurationMinutes();
            services = pricingResult.getServices();
        } else if (serviceIds != null && !serviceIds.isEmpty()) {
            BookingPricingResult pricingResult = bookingPricingService.calculate(branchId, serviceIds, null);
            totalDuration = pricingResult.getTotalDurationMinutes();
            services = pricingResult.getServices();
        } else {
            return AvailabilityResponse.builder().availableStartTimes(new ArrayList<>()).build();
        }

        if (totalDuration == 0) {
            return AvailabilityResponse.builder().availableStartTimes(new ArrayList<>()).build();
        }

        // Kiểm tra xem chi nhánh / Salon có đóng cửa nghỉ lễ vào ngày này không
        if (systemOffDayService != null && systemOffDayService.isBranchClosedOnDate(branchId, date)) {
            return AvailabilityResponse.builder().availableStartTimes(new ArrayList<>()).build();
        }

        // Lấy thời gian mở cửa / đóng cửa của chi nhánh
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId));
        int dbDayOfWeek = date.getDayOfWeek().getValue() == 7 ? 0 : date.getDayOfWeek().getValue();
        Optional<BranchHour> branchHourOpt = branchHourRepository.findByBranchIdAndDayOfWeek(branchId, dbDayOfWeek);
        if (branchHourOpt.isEmpty() || Boolean.TRUE.equals(branchHourOpt.get().getIsClosed())) {
            return AvailabilityResponse.builder().availableStartTimes(new ArrayList<>()).build();
        }
        BranchHour branchHour = branchHourOpt.get();
        LocalTime openTime = branchHour.getOpenTime();
        LocalTime closeTime = branchHour.getCloseTime();

        // --- Tích hợp Redis Cache ---
        String cacheKey = String.format("availability:branch:%d:staff:%s:date:%s:duration:%d",
                branchId,
                staffId != null ? staffId.toString() : "all",
                date.toString(),
                totalDuration
        );

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("Redis cache hit for key: {}", cacheKey);
                List<LocalTime> times = objectMapper.readValue(
                        cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, LocalTime.class)
                );
                return AvailabilityResponse.builder()
                        .availableStartTimes(times)
                        .openTime(openTime)
                        .closeTime(closeTime)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to read from Redis cache", e);
        }

        // 2. Lấy danh sách nhân viên đủ khả năng thực hiện
        List<Staff> branchStaff = staffRepository.findByBranchId(branchId);
        if (staffId != null) {
            branchStaff = branchStaff.stream().filter(s -> s.getId().equals(staffId)).toList();
        }
        
        List<SalonService> finalServices = services;
        List<Staff> qualifiedStaff = branchStaff.stream()
                .filter(s -> isStaffQualified(s, finalServices))
                .toList();

        if (qualifiedStaff.isEmpty()) {
            return AvailabilityResponse.builder().availableStartTimes(new ArrayList<>()).build();
        }

        // 4. Lấy tất cả lịch hẹn hoạt động trong ngày của chi nhánh để tối ưu hóa kiểm tra chéo trong bộ nhớ
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);
        List<Booking> branchBookings = bookingRepository.findByBranchIdAndBookingDateAndStatusIn(branchId, date, activeStatuses);

        // Pre-fetch off-days and scheduled shifts for qualified staff
        java.util.Map<Long, Boolean> staffOffDaysMap = new java.util.HashMap<>();
        java.util.Map<Long, List<Shift>> staffShiftsMap = new java.util.HashMap<>();

        for (Staff staff : qualifiedStaff) {
            boolean isOff = staffOffDayRepository != null && staffOffDayRepository.isStaffApprovedOffOnDate(staff.getId(), date);
            staffOffDaysMap.put(staff.getId(), isOff);

            if (!isOff && staff.getUserId() != null) {
                List<Shift> shifts = shiftRepository.findByUserIdAndShiftDate(staff.getUserId(), date).stream()
                        .filter(s -> s.getStatus() == ShiftStatus.SCHEDULED)
                        .toList();
                staffShiftsMap.put(staff.getId(), shifts);
            } else {
                staffShiftsMap.put(staff.getId(), new ArrayList<>());
            }
        }

        // 5. Quét các khung giờ cách nhau 15 phút
        List<LocalTime> availableStartTimes = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime current = openTime;
        LocalTime lastPossibleStart = closeTime.minusMinutes(totalDuration);

        while (!current.isAfter(lastPossibleStart)) {
            LocalTime slotStart = current;
            LocalTime slotEnd = current.plusMinutes(totalDuration);

            // 0. Nếu là ngày hôm nay, bỏ qua các khung giờ đã trôi qua trong quá khứ
            if (date.equals(today) && slotStart.isBefore(now)) {
                current = current.plusMinutes(15);
                continue;
            }

            // Kiểm tra xem có ít nhất một nhân viên đủ điều kiện đang trống lịch trong khung giờ này không
            boolean anyStaffFree = false;
            for (Staff staff : qualifiedStaff) {
                // 1. Kiểm tra ngày nghỉ
                if (Boolean.TRUE.equals(staffOffDaysMap.get(staff.getId()))) {
                    continue;
                }

                // 2. Kiểm tra ca làm việc (Shift) phải phủ kín slot
                List<Shift> shifts = staffShiftsMap.get(staff.getId());
                boolean coveredByShift = false;
                if (shifts == null || shifts.isEmpty()) {
                    // Nếu chưa được xếp ca làm việc riêng -> Mặc định phục vụ theo giờ mở cửa chi nhánh
                    coveredByShift = !slotStart.isBefore(openTime) && !slotEnd.isAfter(closeTime);
                } else {
                    for (Shift shift : shifts) {
                        if (!slotStart.isBefore(shift.getStartTime()) && !slotEnd.isAfter(shift.getEndTime())) {
                            coveredByShift = true;
                            break;
                        }
                    }
                }
                if (!coveredByShift) {
                    continue;
                }

                // 3. Kiểm tra trùng lịch đặt (Booking)
                boolean staffBusy = false;
                for (Booking booking : branchBookings) {
                    if (booking.getAssignedStaff() != null && booking.getAssignedStaff().getId().equals(staff.getId())) {
                        // Kiểm tra trùng lắp: b.startTime < slotEnd AND slotStart < b.endTime
                        if (booking.getStartTime().isBefore(slotEnd) && slotStart.isBefore(booking.getEndTime())) {
                            staffBusy = true;
                            break;
                        }
                    }
                }
                if (staffBusy) {
                    continue;
                }

                // 4. Kiểm tra Redis Slot Lock của user khác (chống trùng lịch thời gian thực)
                String lockKey = String.format("slot:%d:%d:%s:%s",
                        branchId,
                        staff.getId(),
                        date.toString(),
                        slotStart.toString()
                );
                if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                    continue;
                }

                // Vượt qua tất cả điều kiện
                anyStaffFree = true;
                break;
            }

            if (anyStaffFree) {
                availableStartTimes.add(slotStart);
            }
            current = current.plusMinutes(15);
        }

        // Cache kết quả vào Redis với TTL 60s
        try {
            String json = objectMapper.writeValueAsString(availableStartTimes);
            redisTemplate.opsForValue().set(cacheKey, json, java.time.Duration.ofSeconds(60));
            log.info("Cached availability results to Redis with key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Failed to write to Redis cache", e);
        }

        return AvailabilityResponse.builder()
                .availableStartTimes(availableStartTimes)
                .openTime(openTime)
                .closeTime(closeTime)
                .build();
    }

    // Kiểm tra xem một nhân viên có được cấp phép làm toàn bộ các dịch vụ yêu cầu hay không
    private boolean isStaffQualified(Staff staff, List<SalonService> requiredServices) {
        if (staff.getServices() == null || staff.getServices().isEmpty()) {
            // Nếu chưa gán danh mục dịch vụ riêng cho nhân viên -> Mặc định làm được tất cả dịch vụ chi nhánh
            return true;
        }
        List<Long> allowedServiceIds = staff.getServices().stream()
                .map(SalonService::getId)
                .toList();
        for (SalonService req : requiredServices) {
            if (!allowedServiceIds.contains(req.getId())) {
                return false;
            }
        }
        return true;
    }

    // Lấy thông tin người dùng hiện tại đang đăng nhập
    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
    }

    // Chuyển đổi đối tượng thực thể sang định dạng phản hồi trả về
    private BookingResponse toResponse(Booking booking) {
        List<BookingItemResponse> itemResponses = booking.getItems().stream()
                .map(item -> BookingItemResponse.builder()
                        .id(item.getId())
                        .serviceId(item.getService() != null ? item.getService().getId() : null)
                        .serviceName(item.getService() != null ? item.getService().getName() : null)
                        .bundleId(item.getBundle() != null ? item.getBundle().getId() : null)
                        .bundleName(item.getBundle() != null ? item.getBundle().getName() : null)
                        .price(item.getPrice())
                        .durationMinutes(item.getDurationMinutes())
                        .build())
                .toList();

        com.example.salonflow.ai.dto.noshow.NoShowPredictionDto predictionDto = null;
        try {
            predictionDto = noShowPredictionService.getPredictionByBookingId(booking.getId());
        } catch (Exception ignored) {}

        LocalDateTime reviewedAt = booking.getReviewedAt();
        if (reviewedAt == null && reviewRepository.existsByBookingId(booking.getId())) {
            reviewedAt = LocalDateTime.now();
        }

        String customerPhone = (booking.getCustomer() != null && booking.getCustomer().getPhone() != null && !booking.getCustomer().getPhone().isBlank())
                ? booking.getCustomer().getPhone()
                : ((booking.getCustomer() != null && booking.getCustomer().getUsername() != null && booking.getCustomer().getUsername().matches("^0[0-9]{9,10}$")) ? booking.getCustomer().getUsername() : null);

        return BookingResponse.builder()
                .id(booking.getId())
                .customerId(booking.getCustomer().getId())
                .customerName(booking.getCustomer().getFullName())
                .customerPhone(customerPhone)
                .branchId(booking.getBranch().getId())
                .branchName(booking.getBranch().getName())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .preferredStaffId(booking.getPreferredStaff() != null ? booking.getPreferredStaff().getId() : null)
                .preferredStaffName(booking.getPreferredStaff() != null ? booking.getPreferredStaff().getName() : null)
                .assignedStaffId(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getId() : null)
                .assignedStaffName(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getName() : null)
                .status(booking.getStatus().name())
                .totalPrice(booking.getTotalPrice())
                .depositAmount(booking.getDepositAmount())
                .remainingAmount(booking.getRemainingAmount())
                .totalDurationMinutes(booking.getTotalDurationMinutes())
                .notes(booking.getNotes())
                .invoiceUrl(booking.getInvoiceUrl())
                .invoiceGeneratedAt(booking.getInvoiceGeneratedAt())
                .reviewedAt(reviewedAt)
                .checkedInAt(booking.getCheckedInAt())
                .items(itemResponses)
                .noShowPrediction(predictionDto)
                .build();
    }

    private BigDecimal calculateDepositAmount(List<SalonService> services) {
        return BigDecimal.ZERO;
    }

    private void publishBookingCreatedEvent(Booking booking) {
        publishBookingNotification(
                booking,
                BookingNotificationType.BOOKING_CREATED,
                "Đặt lịch thành công #" + booking.getId(),
                "Lịch hẹn của bạn vào " + booking.getBookingDate() + " lúc " + booking.getStartTime()
                        + " đã được tạo thành công."
        );
    }

    private void publishBookingCancelledEvent(Booking booking, String reason, CancellationResult result) {
        StringBuilder message = new StringBuilder()
                .append("Lịch hẹn của bạn vào ")
                .append(booking.getBookingDate())
                .append(" lúc ")
                .append(booking.getStartTime())
                .append(" đã bị hủy.");

        if (reason != null && !reason.trim().isEmpty()) {
            message.append(" Lý do: ").append(reason.trim()).append(".");
        }

        if (result != null && result.getRefundAmount() != null) {
            message.append(" Số tiền hoàn lại: ").append(result.getRefundAmount()).append(" VND.");
        }

        publishBookingNotification(
                booking,
                BookingNotificationType.BOOKING_CANCELLED,
                "Lịch hẹn #" + booking.getId() + " đã bị hủy",
                message.toString()
        );
    }

    private void publishBookingNotification(
            Booking booking,
            BookingNotificationType type,
            String title,
            String message
    ) {
        try {
            List<Long> recipientUserIds = new ArrayList<>();
            recipientUserIds.add(booking.getCustomer().getId());
            if (booking.getAssignedStaff() != null && booking.getAssignedStaff().getUserId() != null
                    && !booking.getAssignedStaff().getUserId().equals(booking.getCustomer().getId())) {
                recipientUserIds.add(booking.getAssignedStaff().getUserId());
            }

            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("bookingId", booking.getId());
            payload.put("branchId", booking.getBranch() != null ? booking.getBranch().getId() : null);
            payload.put("branchName", booking.getBranch() != null ? booking.getBranch().getName() : null);
            payload.put("customerId", booking.getCustomer() != null ? booking.getCustomer().getId() : null);
            payload.put("customerName", booking.getCustomer() != null ? booking.getCustomer().getFullName() : null);
            payload.put("assignedStaffId", booking.getAssignedStaff() != null ? booking.getAssignedStaff().getId() : null);
            payload.put("assignedStaffUserId", booking.getAssignedStaff() != null ? booking.getAssignedStaff().getUserId() : null);
            payload.put("status", booking.getStatus() != null ? booking.getStatus().name() : null);
            payload.put("bookingDate", booking.getBookingDate());
            payload.put("startTime", booking.getStartTime());
            payload.put("endTime", booking.getEndTime());
            payload.put("totalPrice", booking.getTotalPrice());
            payload.put("depositAmount", booking.getDepositAmount());
            payload.put("remainingAmount", booking.getRemainingAmount());
            payload.put("totalDurationMinutes", booking.getTotalDurationMinutes());
            payload.put("notes", booking.getNotes());
            payload.put("items", booking.getItems() != null
                    ? booking.getItems().stream().map(item -> {
                        java.util.Map<String, Object> itemPayload = new java.util.LinkedHashMap<>();
                        itemPayload.put("serviceId", item.getService() != null ? item.getService().getId() : null);
                        itemPayload.put("bundleId", item.getBundle() != null ? item.getBundle().getId() : null);
                        itemPayload.put("price", item.getPrice());
                        itemPayload.put("durationMinutes", item.getDurationMinutes());
                        return itemPayload;
                    }).toList()
                    : List.of());

            if (type == BookingNotificationType.BOOKING_CANCELLED) {
                payload.put("cancellationReason", message);
            }

            String payloadJson = objectMapper.writeValueAsString(payload);
            applicationEventPublisher.publishEvent(new BookingNotificationEvent(
                    booking.getId(),
                    booking.getBranch() != null ? booking.getBranch().getId() : null,
                    booking.getCustomer() != null ? booking.getCustomer().getId() : null,
                    recipientUserIds,
                    type,
                    title,
                    message,
                    payloadJson
            ));
        } catch (Exception e) {
            log.error("Failed to publish booking notification event for booking {}", booking.getId(), e);
        }
    }

    private BookingResponse createBookingInternal(
            Long branchId,
            User customer,
            LocalDate bookingDate,
            LocalTime startTime,
            Long preferredStaffId,
            List<Long> serviceIds,
            Long bundleId,
            String notes
    ) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId));

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        if (bookingDate.isBefore(today)) {
            throw new BusinessException("Không thể đặt lịch cho ngày trong quá khứ");
        }
        
        if (bookingDate.equals(today) && startTime.isBefore(now)) {
            throw new BusinessException("Thời gian đặt lịch không hợp lệ vì đã qua thời điểm hiện tại");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalDuration = 0;
        List<SalonService> services = new ArrayList<>();
        ServiceBundle bundle = null;

        if (bundleId != null) {
            bundle = serviceBundleRepository.findByIdAndBranchId(bundleId, branchId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy combo với id: " + bundleId + " tại chi nhánh này"));
            totalPrice = bundle.getPrice();
            totalDuration = bundle.getTotalDurationMinutes();
            services = bundle.getItems().stream().map(ServiceBundleItem::getService).toList();
        } else if (serviceIds != null && !serviceIds.isEmpty()) {
            services = serviceRepository.findAllById(serviceIds);
            if (services.size() != serviceIds.size()) {
                throw new BusinessException("Một số dịch vụ được chọn không hợp lệ hoặc không tồn tại");
            }
            for (SalonService service : services) {
                if (!service.getBranch().getId().equals(branchId)) {
                    throw new BusinessException("Dịch vụ '" + service.getName() + "' không thuộc chi nhánh này");
                }
                totalPrice = totalPrice.add(service.getPrice());
                totalDuration += service.getDurationMinutes();
            }
        } else {
            throw new BusinessException("Vui lòng chọn ít nhất một dịch vụ hoặc combo để đặt lịch");
        }

        BigDecimal depositAmount = calculateDepositAmount(services);

        LocalTime endTime = startTime.plusMinutes(totalDuration);

        if (systemOffDayService != null && systemOffDayService.isBranchClosedOnDate(branchId, bookingDate)) {
            throw new BusinessException("Salon/Chi nhánh đóng cửa nghỉ lễ vào ngày " + bookingDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ". Vui lòng chọn ngày khác!");
        }

        int dbDayOfWeek = bookingDate.getDayOfWeek().getValue() == 7 ? 0 : bookingDate.getDayOfWeek().getValue();
        BranchHour branchHour = branchHourRepository.findByBranchIdAndDayOfWeek(branchId, dbDayOfWeek)
                .orElseThrow(() -> new BusinessException("Chi nhánh không có lịch hoạt động vào ngày này"));
        if (Boolean.TRUE.equals(branchHour.getIsClosed())) {
            throw new BusinessException("Chi nhánh đóng cửa vào ngày này");
        }
        if (startTime.isBefore(branchHour.getOpenTime()) || endTime.isAfter(branchHour.getCloseTime())) {
            throw new BusinessException("Thời gian hẹn nằm ngoài khung giờ mở cửa của chi nhánh ("
                    + branchHour.getOpenTime() + " - " + branchHour.getCloseTime() + ")");
        }

        Staff preferredStaff = null;
        Staff assignedStaff = null;
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);

        if (preferredStaffId != null) {
            preferredStaff = staffRepository.findByIdAndBranchId(preferredStaffId, branchId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy nhân viên với id: " + preferredStaffId + " tại chi nhánh này"));

            if (staffOffDayRepository != null && staffOffDayRepository.isStaffApprovedOffOnDate(preferredStaffId, bookingDate)) {
                throw new BusinessException("Nhân viên " + preferredStaff.getName() + " đã xin nghỉ phép vào ngày " + bookingDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ". Vui lòng chọn nhân viên khác hoặc ngày khác!");
            }

            if (!isStaffQualified(preferredStaff, services)) {
                throw new BusinessException("Nhân viên " + preferredStaff.getName() + " không có kỹ năng thực hiện một số dịch vụ đã chọn");
            }

            List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                    preferredStaff.getId(), bookingDate, startTime, endTime, activeStatuses);
            if (!overlapping.isEmpty()) {
                throw new BusinessException("Nhân viên " + preferredStaff.getName() + " đã bận vào khung giờ bạn chọn");
            }
            assignedStaff = preferredStaff;
        } else {
            final List<SalonService> finalServices = services;
            List<Staff> branchStaff = staffRepository.findByBranchId(branchId);
            List<Staff> qualifiedStaff = branchStaff.stream()
                    .filter(s -> isStaffQualified(s, finalServices))
                    .toList();

            if (qualifiedStaff.isEmpty()) {
                throw new BusinessException("Không có nhân viên nào tại chi nhánh có khả năng thực hiện toàn bộ dịch vụ đã chọn");
            }

            List<Staff> availableStaff = new ArrayList<>();
            for (Staff staff : qualifiedStaff) {
                if (staffOffDayRepository != null && staffOffDayRepository.isStaffApprovedOffOnDate(staff.getId(), bookingDate)) {
                    continue;
                }
                List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                        staff.getId(), bookingDate, startTime, endTime, activeStatuses);
                if (overlapping.isEmpty()) {
                    availableStaff.add(staff);
                }
            }

            if (availableStaff.isEmpty()) {
                throw new BusinessException("Hiện tại không có nhân viên nào trống lịch trong khung giờ bạn chọn");
            }

            Staff selectedStaff = availableStaff.get(0);
            int minBookings = Integer.MAX_VALUE;
            for (Staff staff : availableStaff) {
                int count = bookingRepository.findByAssignedStaffIdAndBookingDateAndStatusIn(
                        staff.getId(), bookingDate, activeStatuses).size();
                if (count < minBookings) {
                    minBookings = count;
                    selectedStaff = staff;
                }
            }
            assignedStaff = selectedStaff;
        }

        String lockKey = String.format("lock:booking:%d:%s:%s",
                assignedStaff.getId(),
                bookingDate,
                startTime
        );
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", java.time.Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException("Khung giờ này đang có giao dịch đặt lịch song song hoặc đã được đặt. Vui lòng chọn khung giờ khác.");
        }

        Booking booking = null;
        try {
            booking = Booking.builder()
                    .customer(customer)
                    .branch(branch)
                    .bookingDate(bookingDate)
                    .startTime(startTime)
                    .endTime(endTime)
                    .preferredStaff(preferredStaff)
                    .assignedStaff(assignedStaff)
                    .status(BookingStatus.CONFIRMED)
                    .totalPrice(totalPrice)
                    .depositAmount(depositAmount)
                    .totalDurationMinutes(totalDuration)
                    .notes(notes)
                    .build();

            booking = bookingRepository.save(booking);

            try {
                List<SmartSchedulingLog> logs = smartSchedulingLogRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
                if (!logs.isEmpty()) {
                    SmartSchedulingLog latestLog = logs.get(0);
                    latestLog.setIsBooked(true);
                    latestLog.setSelectedSlotTime(startTime != null ? startTime.toString() : null);
                    if (customer != null && latestLog.getCustomerId() == null) {
                        latestLog.setCustomerId(customer.getId());
                    }
                    smartSchedulingLogRepository.save(latestLog);
                }
            } catch (Exception ex) {
                log.error("Failed to update AI scheduling log booking status: {}", ex.getMessage());
            }

            List<BookingItem> items = new ArrayList<>();
            if (bundle != null) {
                BookingItem item = BookingItem.builder()
                        .booking(booking)
                        .bundle(bundle)
                        .price(bundle.getPrice())
                        .durationMinutes(bundle.getTotalDurationMinutes())
                        .build();
                items.add(bookingItemRepository.save(item));
            } else {
                for (SalonService service : services) {
                    BookingItem item = BookingItem.builder()
                            .booking(booking)
                            .service(service)
                            .price(service.getPrice())
                            .durationMinutes(service.getDurationMinutes())
                            .build();
                    items.add(bookingItemRepository.save(item));
                }
            }
            booking.setItems(items);

            publishBookingCreatedEvent(booking);

            // Send booking confirmation email upon creation (invoice & payment email will be sent when COMPLETED)
            try {
                emailService.sendBookingConfirmationEmail(booking);
                log.info("Booking confirmation email sent for booking ID: {}", booking.getId());
            } catch (Exception ex) {
                log.error("Failed to send booking confirmation email for booking ID: {}", booking.getId(), ex);
            }

            try {
                noShowPredictionService.predictAndSaveLog(booking);
            } catch (Exception e) {
                log.error("Lỗi khi thực hiện AI dự đoán No-Show cho Booking ID: {}", booking.getId(), e);
            }

            try {
                String pattern = String.format("availability:branch:%d:staff:*:date:%s:duration:*",
                        branchId,
                        booking.getBookingDate());
                java.util.Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("Evicted {} availability cache keys for branch {} and date {}", keys.size(), branchId, booking.getBookingDate());
                }
                bookingWebSocketHandler.broadcastBookingUpdate(
                        branchId,
                        booking.getAssignedStaff() != null ? booking.getAssignedStaff().getId() : null,
                        booking.getBookingDate().toString()
                );
            } catch (Exception e) {
                log.error("Failed to evict cache and broadcast booking update", e);
            }
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            redisTemplate.delete(lockKey);
            log.warn("Trùng lặp dữ liệu lịch hẹn khi lưu DB: {}", e.getMessage());
            throw new BusinessException("Khung giờ đặt lịch này vừa được người khác đăng ký hoặc dữ liệu đã tồn tại trong hệ thống. Vui lòng chọn khung giờ khác.");
        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            throw e;
        }

        return toResponse(booking);
    }

    private User findOrCreateGuestCustomer(CreateGuestBookingRequest request) {
        String phoneStr = (request.getCustomerPhone() != null) ? request.getCustomerPhone().trim() : "";
        if (!phoneStr.isBlank()) {
            Optional<User> byPhone = userRepository.findFirstByPhoneOrderByCreatedAtDesc(phoneStr);
            if (byPhone.isPresent()) {
                return byPhone.get();
            }
        }

        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(request.getCustomerEmail().trim());
            if (byEmail.isPresent()) {
                User u = byEmail.get();
                if (!phoneStr.isBlank() && (u.getPhone() == null || u.getPhone().isBlank())) {
                    u.setPhone(phoneStr);
                    u = userRepository.save(u);
                }
                return u;
            }
        }

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role CUSTOMER"));

        String phone = request.getCustomerPhone().trim();
        String email = (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank())
                ? request.getCustomerEmail().trim()
                : "guest_" + UUID.randomUUID() + "@guest.local";
        String username = "guest_" + UUID.randomUUID();

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("Guest@" + UUID.randomUUID()))
                .fullName(request.getCustomerName())
                .phone(phone)
                .status(com.example.salonflow.entity.enums.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(user.getId(), customerRole.getId()))
                .user(user)
                .role(customerRole)
                .assignedAt(LocalDateTime.now())
                .build();
        userRoleRepository.save(userRole);

        return user;
    }
@Override
@Transactional
public CancellationResult cancelBooking(Long bookingId, String reason) {
    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking với ID: " + bookingId));

    if (booking.getStatus() == BookingStatus.CANCELLED) {
        throw new BadRequestException("Booking này đã bị hủy trước đó");
    }

    CancellationPolicy policy = cancellationPolicyRepository.findBySalonId(
            booking.getBranch().getSalon().getId())
            .orElseGet(() -> createDefaultPolicy(booking.getBranch().getSalon()));

    CancellationResult result = calculateCancellationFee(booking, policy);

    PaymentResponse refundedPayment = null;
    if (result.isFreeCancel()) {
        result.setRefundAmount(BigDecimal.ZERO);
    }

    paymentRepository.findByBookingId(bookingId).stream()
            .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
            .forEach(payment -> {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
            });

    booking.setStatus(BookingStatus.CANCELLED);
    if (reason != null && !reason.trim().isEmpty()) {
        booking.setNotes(reason);
    }
    bookingRepository.save(booking);
    publishBookingCancelledEvent(booking, reason, result);

    // Evict cache and broadcast WebSocket update
    try {
        Long branchId = booking.getBranch().getId();
        String pattern = String.format("availability:branch:%d:staff:*:date:%s:duration:*", branchId, booking.getBookingDate().toString());
        java.util.Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} availability cache keys for branch {} and date {} due to cancellation", keys.size(), branchId, booking.getBookingDate());
        }
        bookingWebSocketHandler.broadcastBookingUpdate(
                branchId,
                booking.getAssignedStaff() != null ? booking.getAssignedStaff().getId() : null,
                booking.getBookingDate().toString()
        );
    } catch (Exception e) {
        log.error("Failed to evict cache and broadcast booking cancellation", e);
    }

    return result;
}

    private CancellationResult calculateCancellationFee(Booking booking, CancellationPolicy policy) {
        LocalDateTime bookingTime = booking.getBookingDate().atTime(booking.getStartTime());
        long hoursUntilBooking = java.time.Duration.between(LocalDateTime.now(), bookingTime).toHours();
        BigDecimal depositAmount = booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO;

        if (hoursUntilBooking >= policy.getFreeCancelHours()) {
            return CancellationResult.builder()
                    .success(true)
                    .feeAmount(BigDecimal.ZERO)
                    .refundAmount(depositAmount)
                    .isFreeCancel(true)
                    .message("Hủy miễn phí theo chính sách")
                    .build();
        } else {
            BigDecimal fee = booking.getTotalPrice()
                    .multiply(policy.getFeePercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            return CancellationResult.builder()
                    .success(true)
                    .feeAmount(fee)
                    .refundAmount(BigDecimal.ZERO)
                    .isFreeCancel(false)
                    .message("Hủy có phí: " + fee + " VND (" + policy.getFeePercentage() + "%)")
                    .build();
        }
    }

    private CancellationPolicy createDefaultPolicy(Salon salon) {
        CancellationPolicy policy = CancellationPolicy.builder()
                .salon(salon)
                .freeCancelHours(24)
                .feePercentage(BigDecimal.valueOf(10.0))
                .isActive(true)
                .build();
        return cancellationPolicyRepository.save(policy);
    }

 private User getOrCreateWalkInCustomer(
        CreateWalkInBookingRequest request,
        Branch branch
) {
    String phoneStr = (request.getCustomerPhone() != null) ? request.getCustomerPhone().trim() : "";

    // Nếu số điện thoại đã tồn tại thì dùng luôn
    User existedUser = !phoneStr.isBlank() 
            ? userRepository.findFirstByPhoneOrderByCreatedAtDesc(phoneStr).orElse(null)
            : null;

    if (existedUser != null) {
        return existedUser;
    }

    Role customerRole = roleRepository.findByCode("CUSTOMER")
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Không tìm thấy role CUSTOMER"));

    String uniqueSuffix = System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 6);

    User user = User.builder()
            .username("walkin_" + uniqueSuffix)
            .email("walkin_" + uniqueSuffix + "@walkin.local")
            .passwordHash(passwordEncoder.encode("WalkIn@123"))
            .fullName(request.getCustomerName())
            .phone(request.getCustomerPhone())
            .status(com.example.salonflow.entity.enums.UserStatus.ACTIVE)
            .build();

    user = userRepository.save(user);

    UserRole userRole = UserRole.builder()
            .id(new UserRoleId(user.getId(), customerRole.getId()))
            .user(user)
            .role(customerRole)
            .assignedAt(LocalDateTime.now())
            .build();

    userRoleRepository.save(userRole);

    CustomerProfile profile = CustomerProfile.builder()
            .user(user)
            .salon(branch.getSalon())
            .membershipCode("MEM_" + uniqueSuffix)
            .build();

    customerProfileRepository.save(profile);

    return user;
  }
    @Override
@Transactional
public BookingResponse createWalkInBooking(
        Long branchId,
        CreateWalkInBookingRequest request
) {

    Branch branch = branchRepository.findById(branchId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Không tìm thấy chi nhánh"));

    User customer = getOrCreateWalkInCustomer(request, branch);

    CreateBookingRequest bookingRequest = CreateBookingRequest.builder()
            .customerId(customer.getId())
            .bookingDate(request.getBookingDate())
            .startTime(request.getStartTime())
            .preferredStaffId(request.getPreferredStaffId())
            .serviceIds(request.getServiceIds())
            .bundleId(request.getBundleId())
            .notes(request.getNotes())
            .build();

    return create(branchId, bookingRequest);
 }

    @Override
    @Transactional
    public void cancelUnpaidBookings() {
        log.info("Bat dau quet va tu dong huy cac booking chua thanh toan online...");
        Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<Booking> unpaidBookings = bookingRepository.findUnpaidOnlineBookings(cutoff);
        
        for (Booking booking : unpaidBookings) {
            try {
                cancelBooking(booking.getId(), "Tu dong huy do qua han thanh toan truc tuyen (15 phut)");
                log.info("Da tu dong huy booking chua thanh toan online ID: {}", booking.getId());
            } catch (Exception e) {
                log.error("Loi khi tu dong huy booking ID: {}", booking.getId(), e);
            }
        }
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn với id: " + bookingId));

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return toResponse(booking);
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BusinessException("Chỉ booking đã check-in mới được chuyển sang COMPLETED");
        }

        booking.setStatus(BookingStatus.COMPLETED);

        // Generate invoice & send payment completed email ONLY when booking status is COMPLETED
        try {
            String invoiceUrl = invoicePdfService.generateInvoice(booking);
            booking.setInvoiceUrl(invoiceUrl);
            emailService.sendInvoiceEmail(booking, invoiceUrl);
            log.info("Invoice generated and payment email sent for COMPLETED booking ID: {}", booking.getId());
        } catch (Exception ex) {
            log.error("Failed to generate invoice or send payment email for COMPLETED booking ID: {}", booking.getId(), ex);
        }

        booking = bookingRepository.save(booking);

        boolean hasPayment = paymentRepository.findByBookingId(booking.getId()).stream()
                .anyMatch(p -> p.getStatus() == com.example.salonflow.entity.enums.PaymentStatus.SUCCESS);
        
        if (!hasPayment) {
            com.example.salonflow.entity.Payment payment = com.example.salonflow.entity.Payment.builder()
                    .booking(booking)
                    .paymentMethod(com.example.salonflow.entity.enums.PaymentMethod.CASH)
                    .amount(booking.getTotalPrice() != null ? booking.getTotalPrice() : java.math.BigDecimal.ZERO)
                    .status(com.example.salonflow.entity.enums.PaymentStatus.SUCCESS)
                    .idempotencyKey("auto_cash_" + booking.getId() + "_" + System.currentTimeMillis())
                    .gatewayTransactionId("AUTO_CASH")
                    .build();
            paymentRepository.save(payment);
            log.info("Auto-created CASH payment for COMPLETED booking ID: {}", booking.getId());
        }

        if (booking.getCustomer() != null) {
            loyaltyPointService.earnPointsForBooking(
                    booking.getCustomer().getId(),
                    booking.getTotalPrice(),
                    "BOOKING:" + booking.getId()
            );
        }

        publishBookingCompletedEvent(booking);
        broadcastBookingUpdateSafely(booking);

        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse checkInBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn với id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            if (booking.getCheckedInAt() == null) {
                booking.setCheckedInAt(LocalDateTime.now());
                booking = bookingRepository.save(booking);
            }
            return toResponse(booking);
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Chỉ booking đã CONFIRMED mới được check-in");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);
        broadcastBookingUpdateSafely(booking);

        return toResponse(booking);
    }

    @Override
    @Transactional
    public CheckInBookingResponse checkInBookingByQr(Long bookingId, String signature) {
        if (!bookingQrSignatureService.verify(bookingId, signature)) {
            throw new InvalidTokenException("QR check-in không hợp lệ hoặc đã bị thay đổi");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn với id: " + bookingId));

        String message;
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            if (booking.getCheckedInAt() == null) {
                booking.setCheckedInAt(LocalDateTime.now());
                booking = bookingRepository.save(booking);
            }
            message = "Booking đã được check-in trước đó";
        } else {
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                throw new BusinessException("Chỉ booking đã CONFIRMED mới được check-in");
            }

            booking.setStatus(BookingStatus.CHECKED_IN);
            booking.setCheckedInAt(LocalDateTime.now());
            booking = bookingRepository.save(booking);
            broadcastBookingUpdateSafely(booking);
            message = "Check-in thành công";
        }

        return toCheckInResponse(booking, message);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn với id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return toResponse(booking);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Chỉ booking đang PENDING mới được xác nhận");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        publishBookingNotification(
                booking,
                BookingNotificationType.BOOKING_CONFIRMED,
                "Lịch hẹn #" + booking.getId() + " đã được xác nhận",
                "Lịch hẹn của bạn vào " + booking.getBookingDate() + " lúc " + booking.getStartTime() + " đã được xác nhận thành công."
        );

        broadcastBookingUpdateSafely(booking);

        return toResponse(booking);
    }

    private void publishBookingCompletedEvent(Booking booking) {
        publishBookingNotification(
                booking,
                BookingNotificationType.BOOKING_COMPLETED,
                "Lịch hẹn #" + booking.getId() + " đã hoàn thành",
                "Lịch hẹn của bạn vào " + booking.getBookingDate() + " lúc " + booking.getStartTime() + " đã được đánh dấu hoàn thành."
        );
    }

    private void broadcastBookingUpdateSafely(Booking booking) {
        try {
            bookingWebSocketHandler.broadcastBookingUpdate(
                    booking.getBranch() != null ? booking.getBranch().getId() : null,
                    booking.getAssignedStaff() != null ? booking.getAssignedStaff().getId() : null,
                    booking.getBookingDate() != null ? booking.getBookingDate().toString() : null
            );
        } catch (Exception e) {
            log.error("Failed to broadcast booking update for booking {}", booking.getId(), e);
        }
    }

    private CheckInBookingResponse toCheckInResponse(Booking booking, String message) {
        return CheckInBookingResponse.builder()
                .success(true)
                .message(message)
                .bookingId(booking.getId())
                .status(booking.getStatus().name())
                .checkedInAt(booking.getCheckedInAt())
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getFullName() : null)
                .customerPhone(booking.getCustomer() != null ? booking.getCustomer().getPhone() : null)
                .branchId(booking.getBranch() != null ? booking.getBranch().getId() : null)
                .branchName(booking.getBranch() != null ? booking.getBranch().getName() : null)
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .assignedStaffId(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getId() : null)
                .assignedStaffName(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getName() : null)
                .build();
    }
}
