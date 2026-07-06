package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.booking.CreateBookingRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.salonflow.dto.booking.CancellationResult;
import com.example.salonflow.exception.BadRequestException;

/**
 * Lớp triển khai các nghiệp vụ liên quan đến Đặt lịch hẹn (BookingService).
 * Sử dụng tên đầy đủ của chú thích @Service để tránh xung đột với thực thể Service của dự án.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
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
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với id: " + request.getCustomerId()));
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

        // 7. Tạo lịch hẹn Booking chính
        Booking booking = Booking.builder()
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

        // 3. Lấy thời gian mở cửa / đóng cửa của chi nhánh
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

        // 4. Lấy tất cả lịch hẹn hoạt động trong ngày của chi nhánh để tối ưu hóa kiểm tra chéo trong bộ nhớ
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
        List<Booking> branchBookings = bookingRepository.findByBranchIdAndBookingDateAndStatusIn(branchId, date, activeStatuses);

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
                if (!staffBusy) {
                    anyStaffFree = true;
                    break;
                }
            }

            if (anyStaffFree) {
                availableStartTimes.add(slotStart);
            }
            current = current.plusMinutes(15);
        }

        return AvailabilityResponse.builder()
                .availableStartTimes(availableStartTimes)
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
}
