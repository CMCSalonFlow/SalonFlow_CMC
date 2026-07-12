package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.booking.CreateBookingRequest;
import com.example.salonflow.dto.booking.CreateWalkInBookingRequest;
import com.example.salonflow.dto.booking.BookingResponse;
import com.example.salonflow.dto.booking.BookingItemResponse;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.BookingService;
import com.example.salonflow.services.service.EmailService;
import com.example.salonflow.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.entity.enums.ShiftStatus;
import com.example.salonflow.websocket.BookingWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private final EmailService emailService; // ← Thêm nếu chưa có
    private final ShiftRepository shiftRepository;
    private final StaffOffDayRepository staffOffDayRepository;
    private final BookingWebSocketHandler bookingWebSocketHandler;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final CustomerProfileRepository customerProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public BookingResponse create(Long branchId, CreateBookingRequest request) {
        // 1. Xác thực chi nhánh có tồn tại hay không
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId));

        // 2. Xác định khách hàng thực hiện đặt lịch
    User customer;

if (request.getCustomerId() != null) {

    customer = userRepository.findById(request.getCustomerId())
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Không tìm thấy khách hàng với id: " + request.getCustomerId()));

} else {

    customer = getCurrentUser();
}

        // 3. Tính toán tổng thời lượng, tổng số tiền và lấy danh sách chi tiết dịch vụ/combo
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalDuration = 0;
        List<SalonService> services = new ArrayList<>();
        ServiceBundle bundle = null;

        if (request.getBundleId() != null) {
            // Đặt lịch theo combo (bundle)
            bundle = serviceBundleRepository.findByIdAndBranchId(request.getBundleId(), branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy combo với id: " + request.getBundleId() + " tại chi nhánh này"));
            totalPrice = bundle.getPrice();
            totalDuration = bundle.getTotalDurationMinutes();
            // Lấy danh sách dịch vụ trong combo
            services = bundle.getItems().stream().map(ServiceBundleItem::getService).toList();
        } else if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            // Đặt lịch theo danh sách dịch vụ lẻ
            services = serviceRepository.findAllById(request.getServiceIds());
            if (services.size() != request.getServiceIds().size()) {
                throw new BusinessException("Một số dịch vụ được chọn không hợp lệ hoặc không tồn tại");
            }
            // Đảm bảo tất cả các dịch vụ đều thuộc chi nhánh này
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

        // 4. Tính toán thời gian kết thúc lịch hẹn
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(totalDuration);

        // 5. Kiểm tra thời gian hoạt động của chi nhánh
        int dbDayOfWeek = request.getBookingDate().getDayOfWeek().getValue() == 7 ? 0 : request.getBookingDate().getDayOfWeek().getValue();
        BranchHour branchHour = branchHourRepository.findByBranchIdAndDayOfWeek(branchId, dbDayOfWeek)
                .orElseThrow(() -> new BusinessException("Chi nhánh không có lịch hoạt động vào ngày này"));
        if (Boolean.TRUE.equals(branchHour.getIsClosed())) {
            throw new BusinessException("Chi nhánh đóng cửa vào ngày này");
        }
        if (startTime.isBefore(branchHour.getOpenTime()) || endTime.isAfter(branchHour.getCloseTime())) {
            throw new BusinessException("Thời gian hẹn nằm ngoài khung giờ mở cửa của chi nhánh (" 
                    + branchHour.getOpenTime() + " - " + branchHour.getCloseTime() + ")");
        }

        // 6. Phân bổ nhân viên thực hiện (Staff Allocation)
        Staff preferredStaff = null;
        Staff assignedStaff = null;
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

        if (request.getPreferredStaffId() != null) {
            // Trường hợp khách hàng yêu cầu chọn nhân viên cụ thể
            preferredStaff = staffRepository.findByIdAndBranchId(request.getPreferredStaffId(), branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với id: " + request.getPreferredStaffId() + " tại chi nhánh này"));
            
            // Kiểm tra xem nhân viên này có kỹ năng thực hiện các dịch vụ đã chọn không
            if (!isStaffQualified(preferredStaff, services)) {
                throw new BusinessException("Nhân viên " + preferredStaff.getName() + " không có kỹ năng thực hiện một số dịch vụ đã chọn");
            }

            // Kiểm tra xem nhân viên có bị trùng lịch hẹn nào khác không
            List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                    preferredStaff.getId(), request.getBookingDate(), startTime, endTime, activeStatuses);
            if (!overlapping.isEmpty()) {
                throw new BusinessException("Nhân viên " + preferredStaff.getName() + " đã bận vào khung giờ bạn chọn");
            }
            assignedStaff = preferredStaff;
        } else {
            // Trường hợp chọn "Bất kỳ nhân viên" -> Phân bổ tự động
            final List<SalonService> finalServices = services;
            List<Staff> branchStaff = staffRepository.findByBranchId(branchId);
            List<Staff> qualifiedStaff = branchStaff.stream()
                    .filter(s -> isStaffQualified(s, finalServices))
                    .toList();

            if (qualifiedStaff.isEmpty()) {
                throw new BusinessException("Không có nhân viên nào tại chi nhánh có khả năng thực hiện toàn bộ dịch vụ đã chọn");
            }

            // Lọc ra các nhân viên đang rảnh trong khung giờ này
            List<Staff> availableStaff = new ArrayList<>();
            for (Staff staff : qualifiedStaff) {
                List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                        staff.getId(), request.getBookingDate(), startTime, endTime, activeStatuses);
                if (overlapping.isEmpty()) {
                    availableStaff.add(staff);
                }
            }

            if (availableStaff.isEmpty()) {
                throw new BusinessException("Hiện tại không có nhân viên nào trống lịch trong khung giờ bạn chọn");
            }

            // Thuật toán Least Bookings: Chọn người có ít lượt đặt lịch nhất trong ngày để cân bằng tải
            Staff selectedStaff = availableStaff.get(0);
            int minBookings = Integer.MAX_VALUE;
            for (Staff staff : availableStaff) {
                int count = bookingRepository.findByAssignedStaffIdAndBookingDateAndStatusIn(
                        staff.getId(), request.getBookingDate(), activeStatuses).size();
                if (count < minBookings) {
                    minBookings = count;
                    selectedStaff = staff;
                }
            }
            assignedStaff = selectedStaff;
        }

        // Chống trùng lịch đồng thời (Race condition control via Redis Lock)
        String lockKey = String.format("lock:booking:%d:%s:%s",
                assignedStaff.getId(),
                request.getBookingDate().toString(),
                startTime.toString()
        );
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", java.time.Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException("Khung giờ này đang có giao dịch đặt lịch song song hoặc đã được đặt. Vui lòng chọn khung giờ khác.");
        }

        Booking booking = null;
        try {
            // 7. Tạo lịch hẹn Booking chính
            booking = Booking.builder()
                    .customer(customer)
                    .branch(branch)
                    .bookingDate(request.getBookingDate())
                    .startTime(startTime)
                    .endTime(endTime)
                    .preferredStaff(preferredStaff)
                    .assignedStaff(assignedStaff)
                    .status(BookingStatus.PENDING)
                    .totalPrice(totalPrice)
                    .totalDurationMinutes(totalDuration)
                    .notes(request.getNotes())
                    .build();

            booking = bookingRepository.save(booking);

        // 8. Tạo chi tiết Booking Items
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

        // Evict cache and broadcast WebSocket update
        try {
            Long branchIdOpt = branchId;
            String pattern = String.format("availability:branch:%d:staff:*:date:%s:duration:*", branchIdOpt, booking.getBookingDate().toString());
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} availability cache keys for branch {} and date {}", keys.size(), branchIdOpt, booking.getBookingDate());
            }
            bookingWebSocketHandler.broadcastBookingUpdate(
                    branchIdOpt,
                    booking.getAssignedStaff() != null ? booking.getAssignedStaff().getId() : null,
                    booking.getBookingDate().toString()
            );
        } catch (Exception e) {
            log.error("Failed to evict cache and broadcast booking update", e);
        }

        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            throw e;
        }

        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getByBranch(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId);
        }
        return bookingRepository.findAll().stream()
                .filter(b -> b.getBranch().getId().equals(branchId))
                .map(this::toResponse)
                .toList();
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
            totalDuration = bundle.getTotalDurationMinutes();
            services = bundle.getItems().stream().map(ServiceBundleItem::getService).toList();
        } else if (serviceIds != null && !serviceIds.isEmpty()) {
            services = serviceRepository.findAllById(serviceIds);
            for (SalonService service : services) {
                totalDuration += service.getDurationMinutes();
            }
        } else {
            return AvailabilityResponse.builder().availableStartTimes(new ArrayList<>()).build();
        }

        if (totalDuration == 0) {
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
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
        List<Booking> branchBookings = bookingRepository.findByBranchIdAndBookingDateAndStatusIn(branchId, date, activeStatuses);

        // Pre-fetch off-days and scheduled shifts for qualified staff
        java.util.Map<Long, Boolean> staffOffDaysMap = new java.util.HashMap<>();
        java.util.Map<Long, List<Shift>> staffShiftsMap = new java.util.HashMap<>();

        for (Staff staff : qualifiedStaff) {
            boolean isOff = staffOffDayRepository.existsByStaffIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(
                    staff.getId(), date, date
            );
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
        LocalTime current = openTime;
        LocalTime lastPossibleStart = closeTime.minusMinutes(totalDuration);

        while (!current.isAfter(lastPossibleStart)) {
            LocalTime slotStart = current;
            LocalTime slotEnd = current.plusMinutes(totalDuration);

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
                for (Shift shift : shifts) {
                    if (!slotStart.isBefore(shift.getStartTime()) && !slotEnd.isAfter(shift.getEndTime())) {
                        coveredByShift = true;
                        break;
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

        return BookingResponse.builder()
                .id(booking.getId())
                .customerId(booking.getCustomer().getId())
                .customerName(booking.getCustomer().getFullName())
                .customerPhone(booking.getCustomer().getPhone())
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
                .totalDurationMinutes(booking.getTotalDurationMinutes())
                .notes(booking.getNotes())
                .items(itemResponses)
                .build();
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

    booking.setStatus(BookingStatus.CANCELLED);
    if (reason != null && !reason.trim().isEmpty()) {
        booking.setNotes(reason);
    }
    bookingRepository.save(booking);

    // TODO: Gửi email sau khi hoàn thiện EmailService
    // emailService.sendCancellationEmail(booking, result);

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

        if (hoursUntilBooking >= policy.getFreeCancelHours()) {
            return CancellationResult.builder()
                    .success(true)
                    .feeAmount(BigDecimal.ZERO)
                    .isFreeCancel(true)
                    .message("Hủy miễn phí theo chính sách")
                    .build();
        } else {
            BigDecimal fee = booking.getTotalPrice()
                    .multiply(policy.getFeePercentage())
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

            return CancellationResult.builder()
                    .success(true)
                    .feeAmount(fee)
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

    // Nếu số điện thoại đã tồn tại thì dùng luôn
    User existedUser = userRepository
            .findByPhone(request.getCustomerPhone())
            .orElse(null);

    if (existedUser != null) {
        return existedUser;
    }

    Role customerRole = roleRepository.findByCode("CUSTOMER")
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Không tìm thấy role CUSTOMER"));

    User user = User.builder()
            .username("walkin_" + System.currentTimeMillis())
            .email("walkin_" + System.currentTimeMillis() + "@walkin.local")
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

    user.getUserRoles().add(userRole);

    CustomerProfile profile = CustomerProfile.builder()
            .user(user)
            .salon(branch.getSalon())
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
}
