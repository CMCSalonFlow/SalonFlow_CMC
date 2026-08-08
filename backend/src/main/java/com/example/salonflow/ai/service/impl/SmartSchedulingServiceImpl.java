package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.dto.scheduling.*;
import com.example.salonflow.ai.service.SmartSchedulingService;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.ShiftStatus;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.pricing.BookingPricingResult;
import com.example.salonflow.pricing.BookingPricingService;
import com.example.salonflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartSchedulingServiceImpl implements SmartSchedulingService {

    private final SmartSchedulingConfigRepository configRepository;
    private final SmartSchedulingLogRepository logRepository;
    private final BranchRepository branchRepository;
    private final BranchHourRepository branchHourRepository;
    private final StaffRepository staffRepository;
    private final StaffOffDayRepository staffOffDayRepository;
    private final ShiftRepository shiftRepository;
    private final BookingRepository bookingRepository;
    private final ServiceBundleRepository serviceBundleRepository;
    private final BookingPricingService bookingPricingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Trọng số mặc định nếu chưa cấu hình
    private static final BigDecimal DEFAULT_WORKLOAD_WEIGHT = new BigDecimal("0.400");
    private static final BigDecimal DEFAULT_TRAVEL_WEIGHT = new BigDecimal("0.300");
    private static final BigDecimal DEFAULT_SERVICE_FIT_WEIGHT = new BigDecimal("0.300");

    @Override
    @Transactional
    public SmartSchedulingResponse recommendSlots(SmartSchedulingRequest request) {
        log.info("Bắt đầu xử lý AI Smart Scheduling cho branchId: {}, date: {}", request.getBranchId(), request.getDate());

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh với ID: " + request.getBranchId()));

        // 1. Lấy thông tin thời lượng và danh sách dịch vụ
        int totalDuration = 0;
        List<SalonService> requestedServices = new ArrayList<>();

        if (request.getBundleId() != null) {
            ServiceBundle bundle = serviceBundleRepository.findByIdAndBranchId(request.getBundleId(), request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy combo với id: " + request.getBundleId()));
            BookingPricingResult pricingResult = bookingPricingService.calculate(request.getBranchId(), null, bundle);
            totalDuration = pricingResult.getTotalDurationMinutes();
            requestedServices = pricingResult.getServices();
        } else if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            BookingPricingResult pricingResult = bookingPricingService.calculate(request.getBranchId(), request.getServiceIds(), null);
            totalDuration = pricingResult.getTotalDurationMinutes();
            requestedServices = pricingResult.getServices();
        }

        if (totalDuration <= 0) {
            totalDuration = 30; // Mặc định 30 phút nếu không có thời lượng
        }

        // 2. Lấy giờ mở/đóng cửa chi nhánh
        int dbDayOfWeek = request.getDate().getDayOfWeek().getValue() == 7 ? 0 : request.getDate().getDayOfWeek().getValue();
        Optional<BranchHour> branchHourOpt = branchHourRepository.findByBranchIdAndDayOfWeek(request.getBranchId(), dbDayOfWeek);
        if (branchHourOpt.isEmpty() || Boolean.TRUE.equals(branchHourOpt.get().getIsClosed())) {
            return buildEmptyResponse(request, getActiveConfig(request.getBranchId()));
        }

        BranchHour branchHour = branchHourOpt.get();
        LocalTime openTime = branchHour.getOpenTime();
        LocalTime closeTime = branchHour.getCloseTime();

        // 3. Lấy cấu hình trọng số AI
        SmartSchedulingConfig config = getActiveConfig(request.getBranchId());
        double wWorkload = config.getWorkloadWeight().doubleValue();
        double wTravel = config.getTravelWeight().doubleValue();
        double wServiceFit = config.getServiceFitWeight().doubleValue();

        // 4. Lấy danh sách nhân viên chi nhánh & lọc nhân viên đủ tay nghề
        List<Staff> staffList = staffRepository.findByBranchId(request.getBranchId());
        if (request.getPreferredStaffId() != null) {
            staffList = staffList.stream()
                    .filter(s -> s.getId().equals(request.getPreferredStaffId()))
                    .toList();
        }

        final List<SalonService> finalServices = requestedServices;
        List<Staff> qualifiedStaff = staffList.stream()
                .filter(s -> isStaffQualified(s, finalServices))
                .toList();

        if (qualifiedStaff.isEmpty()) {
            return buildEmptyResponse(request, config);
        }

        // 5. Pre-fetch booking & ca làm việc hiện tại trong ngày
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);
        List<Booking> branchBookings = bookingRepository.findByBranchIdAndBookingDateAndStatusIn(request.getBranchId(), request.getDate(), activeStatuses);

        Map<Long, List<Booking>> staffBookingsMap = new HashMap<>();
        Map<Long, Integer> staffBookedMinutesMap = new HashMap<>();
        Map<Long, List<Shift>> staffShiftsMap = new HashMap<>();
        Map<Long, Boolean> staffOffDaysMap = new HashMap<>();

        for (Staff staff : qualifiedStaff) {
            boolean isOff = staffOffDayRepository.existsByStaffIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(
                    staff.getId(), request.getDate(), request.getDate()
            );
            staffOffDaysMap.put(staff.getId(), isOff);

            List<Booking> sBookings = branchBookings.stream()
                    .filter(b -> b.getAssignedStaff() != null && b.getAssignedStaff().getId().equals(staff.getId()))
                    .sorted(Comparator.comparing(Booking::getStartTime))
                    .toList();
            staffBookingsMap.put(staff.getId(), sBookings);

            int totalBookedMins = sBookings.stream()
                    .mapToInt(b -> b.getTotalDurationMinutes() != null ? b.getTotalDurationMinutes() : (int) Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
                    .sum();
            staffBookedMinutesMap.put(staff.getId(), totalBookedMins);

            if (!isOff && staff.getUserId() != null) {
                List<Shift> shifts = shiftRepository.findByUserIdAndShiftDate(staff.getUserId(), request.getDate()).stream()
                        .filter(s -> s.getStatus() == ShiftStatus.SCHEDULED)
                        .toList();
                staffShiftsMap.put(staff.getId(), shifts);
            } else {
                staffShiftsMap.put(staff.getId(), new ArrayList<>());
            }
        }

        // 6. Quét tìm danh sách các slot khả thi & Tính điểm Scoring
        List<SlotCandidate> candidateSlots = new ArrayList<>();
        LocalTime current = openTime;
        LocalTime lastPossibleStart = closeTime.minusMinutes(totalDuration);

        while (!current.isAfter(lastPossibleStart)) {
            LocalTime slotStart = current;
            LocalTime slotEnd = current.plusMinutes(totalDuration);

            // Kiểm tra bộ lọc khoảng giờ khách mong muốn (nếu có)
            if (request.getPreferredStartTime() != null && slotStart.isBefore(request.getPreferredStartTime())) {
                current = current.plusMinutes(15);
                continue;
            }
            if (request.getPreferredEndTime() != null && slotEnd.isAfter(request.getPreferredEndTime())) {
                current = current.plusMinutes(15);
                continue;
            }

            for (Staff staff : qualifiedStaff) {
                if (Boolean.TRUE.equals(staffOffDaysMap.get(staff.getId()))) {
                    continue;
                }

                // Check shift coverage
                List<Shift> shifts = staffShiftsMap.get(staff.getId());
                boolean coveredByShift = false;
                if (shifts == null || shifts.isEmpty()) {
                    coveredByShift = !slotStart.isBefore(openTime) && !slotEnd.isAfter(closeTime);
                } else {
                    for (Shift shift : shifts) {
                        if (!slotStart.isBefore(shift.getStartTime()) && !slotEnd.isAfter(shift.getEndTime())) {
                            coveredByShift = true;
                            break;
                        }
                    }
                }
                if (!coveredByShift) continue;

                // Check booking conflict
                List<Booking> sBookings = staffBookingsMap.get(staff.getId());
                boolean hasConflict = sBookings.stream().anyMatch(b ->
                        !(slotEnd.isBefore(b.getStartTime()) || slotEnd.equals(b.getStartTime()) ||
                                slotStart.isAfter(b.getEndTime()) || slotStart.equals(b.getEndTime()))
                );
                if (hasConflict) continue;

                // --- TÍNH ĐIỂM SCORE CHO CANDIDATE SLOT ---
                // A. Workload Balance Score
                int currentBookedMins = staffBookedMinutesMap.getOrDefault(staff.getId(), 0);
                int shiftTotalMins = calculateShiftTotalMinutes(shifts, openTime, closeTime);
                double workloadRatio = (double) (currentBookedMins + totalDuration) / Math.max(shiftTotalMins, 240);
                double workloadScore = Math.max(0.0, Math.min(1.0, 1.0 - (workloadRatio * 0.8)));

                // B. Travel & Gap Optimization Score
                double gapScore = calculateGapScore(slotStart, slotEnd, sBookings);
                double distanceScore = calculateDistanceScore(branch, request.getCustomerLatitude(), request.getCustomerLongitude());
                double travelGapScore = (request.getCustomerLatitude() != null && request.getCustomerLongitude() != null)
                        ? (gapScore * 0.7 + distanceScore * 0.3)
                        : gapScore;

                // C. Service Fit Score
                double serviceFitScore = calculateServiceFitScore(staff, finalServices, request.getPreferredStaffId());

                // Total Score
                double rawTotal = (workloadScore * wWorkload) + (travelGapScore * wTravel) + (serviceFitScore * wServiceFit);
                double totalScoreScaled = Math.round(rawTotal * 100.0 * 10.0) / 10.0; // Scale 0-100, 1 chữ số thập phân

                String explanation = generateExplanation(staff, workloadScore, gapScore, serviceFitScore, request.getPreferredStaffId());

                SlotRecommendationDto dto = SlotRecommendationDto.builder()
                        .startTime(slotStart)
                        .endTime(slotEnd)
                        .staffId(staff.getId())
                        .staffName(staff.getName())
                        .staffAvatar(staff.getAvatarUrl())
                        .staffSpecialties(staff.getSpecialties())
                        .totalScore(totalScoreScaled)
                        .workloadBalanceScore(Math.round(workloadScore * 100.0) / 100.0)
                        .travelGapScore(Math.round(travelGapScore * 100.0) / 100.0)
                        .serviceFitScore(Math.round(serviceFitScore * 100.0) / 100.0)
                        .explanation(explanation)
                        .build();

                candidateSlots.add(new SlotCandidate(rawTotal, dto));
            }

            current = current.plusMinutes(15);
        }

        // 7. Sắp xếp danh sách theo totalScore giảm dần và lấy Top 3
        candidateSlots.sort(Comparator.comparingDouble(SlotCandidate::getScore).reversed());

        // Đảm bảo chọn Top 3 đa dạng thời gian (tránh chọn trùng slot cùng thời gian nếu có nhiều staff trừ khi staff quá tốt)
        List<SlotRecommendationDto> top3Recommendations = candidateSlots.stream()
                .map(SlotCandidate::getDto)
                .limit(3)
                .toList();

        // 8. Ghi log lịch sử đề xuất
        SmartSchedulingLog savedLog = saveRecommendationLog(request, config, top3Recommendations);

        SmartSchedulingConfigDto configDto = SmartSchedulingConfigDto.builder()
                .id(config.getId())
                .branchId(config.getBranchId())
                .workloadWeight(config.getWorkloadWeight())
                .travelWeight(config.getTravelWeight())
                .serviceFitWeight(config.getServiceFitWeight())
                .description(config.getDescription())
                .build();

        return SmartSchedulingResponse.builder()
                .logId(savedLog != null ? savedLog.getId() : null)
                .branchId(request.getBranchId())
                .date(request.getDate().toString())
                .weightsUsed(configDto)
                .recommendations(top3Recommendations)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SmartSchedulingConfigDto getConfig(Long branchId) {
        SmartSchedulingConfig config = getActiveConfig(branchId);
        return SmartSchedulingConfigDto.builder()
                .id(config.getId())
                .branchId(config.getBranchId())
                .workloadWeight(config.getWorkloadWeight())
                .travelWeight(config.getTravelWeight())
                .serviceFitWeight(config.getServiceFitWeight())
                .description(config.getDescription())
                .build();
    }

    @Override
    @Transactional
    public SmartSchedulingConfigDto updateConfig(Long branchId, UpdateSmartSchedulingConfigDto dto) {
        BigDecimal sum = dto.getWorkloadWeight().add(dto.getTravelWeight()).add(dto.getServiceFitWeight());
        if (sum.compareTo(new BigDecimal("1.000")) != 0 && sum.compareTo(new BigDecimal("1.00")) != 0 && sum.compareTo(BigDecimal.ONE) != 0) {
            throw new BadRequestException("Tổng các trọng số (Workload + Travel + ServiceFit) phải chính xác bằng 1.0! Tổng hiện tại: " + sum);
        }

        SmartSchedulingConfig config = configRepository.findByBranchId(branchId)
                .orElseGet(() -> SmartSchedulingConfig.builder().branchId(branchId).build());

        config.setWorkloadWeight(dto.getWorkloadWeight().setScale(3, RoundingMode.HALF_UP));
        config.setTravelWeight(dto.getTravelWeight().setScale(3, RoundingMode.HALF_UP));
        config.setServiceFitWeight(dto.getServiceFitWeight().setScale(3, RoundingMode.HALF_UP));
        if (dto.getDescription() != null) {
            config.setDescription(dto.getDescription());
        }

        SmartSchedulingConfig saved = configRepository.save(config);

        return SmartSchedulingConfigDto.builder()
                .id(saved.getId())
                .branchId(saved.getBranchId())
                .workloadWeight(saved.getWorkloadWeight())
                .travelWeight(saved.getTravelWeight())
                .serviceFitWeight(saved.getServiceFitWeight())
                .description(saved.getDescription())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SmartSchedulingLog> getRecommendationLogs(Long branchId, Pageable pageable) {
        if (branchId != null) {
            return logRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable);
        }
        return logRepository.findAll(pageable);
    }

    // --- HELPER METHODS ---

    private SmartSchedulingConfig getActiveConfig(Long branchId) {
        if (branchId != null) {
            Optional<SmartSchedulingConfig> branchConfig = configRepository.findByBranchId(branchId);
            if (branchConfig.isPresent()) return branchConfig.get();
        }

        return configRepository.findFirstByBranchIdIsNull()
                .orElseGet(() -> SmartSchedulingConfig.builder()
                        .workloadWeight(DEFAULT_WORKLOAD_WEIGHT)
                        .travelWeight(DEFAULT_TRAVEL_WEIGHT)
                        .serviceFitWeight(DEFAULT_SERVICE_FIT_WEIGHT)
                        .description("Cấu hình trọng số mặc định toàn hệ thống")
                        .build());
    }

    private boolean isStaffQualified(Staff staff, List<SalonService> services) {
        if (services == null || services.isEmpty()) return true;
        if (staff.getServices() == null || staff.getServices().isEmpty()) return false;

        Set<Long> staffServiceIds = staff.getServices().stream()
                .map(SalonService::getId)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        return services.stream().allMatch(s -> staffServiceIds.contains(s.getId()));
    }

    private int calculateShiftTotalMinutes(List<Shift> shifts, LocalTime openTime, LocalTime closeTime) {
        if (shifts == null || shifts.isEmpty()) {
            return (int) Duration.between(openTime, closeTime).toMinutes();
        }
        return shifts.stream()
                .mapToInt(s -> (int) Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                .sum();
    }

    private double calculateGapScore(LocalTime slotStart, LocalTime slotEnd, List<Booking> staffBookings) {
        if (staffBookings == null || staffBookings.isEmpty()) {
            return 0.9; // Điểm rảnh rỗi tốt
        }

        // Tìm booking kết thúc ngay trước slotStart và booking bắt đầu ngay sau slotEnd
        Optional<Booking> prevBooking = staffBookings.stream()
                .filter(b -> b.getEndTime().isBefore(slotStart) || b.getEndTime().equals(slotStart))
                .max(Comparator.comparing(Booking::getEndTime));

        Optional<Booking> nextBooking = staffBookings.stream()
                .filter(b -> b.getStartTime().isAfter(slotEnd) || b.getStartTime().equals(slotEnd))
                .min(Comparator.comparing(Booking::getStartTime));

        double prevGapMins = prevBooking.map(booking -> (double) Duration.between(booking.getEndTime(), slotStart).toMinutes()).orElse(120.0);
        double nextGapMins = nextBooking.map(booking -> (double) Duration.between(slotEnd, booking.getStartTime()).toMinutes()).orElse(120.0);

        double minGapMins = Math.min(prevGapMins, nextGapMins);

        if (minGapMins >= 10 && minGapMins <= 30) {
            return 1.0; // Khoảng thời gian nghỉ/chuẩn bị vàng
        } else if (minGapMins == 0) {
            return 0.85; // Ca liền kề kít nút
        } else if (minGapMins > 30 && minGapMins <= 60) {
            return 0.8;
        } else if (minGapMins > 60 && minGapMins <= 120) {
            return 0.65;
        } else {
            return 0.5;
        }
    }

    private double calculateDistanceScore(Branch branch, Double custLat, Double custLng) {
        if (custLat == null || custLng == null || branch.getLatitude() == null || branch.getLongitude() == null) {
            return 1.0;
        }
        double dLat = Math.toRadians(branch.getLatitude() - custLat);
        double dLng = Math.toRadians(branch.getLongitude() - custLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(custLat)) * Math.cos(Math.toRadians(branch.getLatitude())) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = 6371 * c; // Earth radius ~6371 km

        return Math.max(0.1, 1.0 / (1.0 + 0.1 * distanceKm));
    }

    private double calculateServiceFitScore(Staff staff, List<SalonService> services, Long preferredStaffId) {
        double score = 0.8; // Cơ bản đã qualified

        if (preferredStaffId != null && preferredStaffId.equals(staff.getId())) {
            score += 0.2; // Ưu tiên hàng đầu nếu khách chọn chỉ định
        }

        if (staff.getSpecialties() != null && services != null && !services.isEmpty()) {
            String specLower = staff.getSpecialties().toLowerCase();
            boolean matchTag = services.stream().anyMatch(s ->
                    s.getName() != null && specLower.contains(s.getName().toLowerCase())
            );
            if (matchTag) score += 0.1;
        }

        return Math.min(1.0, score);
    }

    private String generateExplanation(Staff staff, double workloadScore, double gapScore, double serviceFitScore, Long preferredStaffId) {
        List<String> reasons = new ArrayList<>();

        if (preferredStaffId != null && preferredStaffId.equals(staff.getId())) {
            reasons.add("Khớp nhân viên chỉ định yêu thích của bạn");
        } else if (serviceFitScore >= 0.9) {
            reasons.add("Kỹ năng chuyên môn của " + staff.getName() + " hoàn toàn tối ưu cho dịch vụ");
        }

        if (workloadScore >= 0.8) {
            reasons.add("Lịch làm việc của nhân viên đang rất cân bằng");
        }

        if (gapScore >= 0.9) {
            reasons.add("Khung giờ có thời gian chuẩn bị lý tưởng (10-30 phút)");
        } else if (gapScore >= 0.8) {
            reasons.add("Khung giờ nối ca thuận tiện");
        }

        if (reasons.isEmpty()) {
            return "Khung giờ phù hợp với lịch làm việc của chi nhánh và nhân viên " + staff.getName() + ".";
        }

        return String.join(", ", reasons) + ".";
    }

    private SmartSchedulingLog saveRecommendationLog(SmartSchedulingRequest request, SmartSchedulingConfig config, List<SlotRecommendationDto> recommendations) {
        try {
            String serviceIdsJson = request.getServiceIds() != null ? objectMapper.writeValueAsString(request.getServiceIds()) : "[]";
            String recSlotsJson = objectMapper.writeValueAsString(recommendations);
            String weightsJson = objectMapper.writeValueAsString(Map.of(
                    "workloadWeight", config.getWorkloadWeight(),
                    "travelWeight", config.getTravelWeight(),
                    "serviceFitWeight", config.getServiceFitWeight()
            ));

            SmartSchedulingLog schedulingLog = SmartSchedulingLog.builder()
                    .branchId(request.getBranchId())
                    .bookingDate(request.getDate())
                    .requestedServiceIds(serviceIdsJson)
                    .bundleId(request.getBundleId())
                    .preferredStaffId(request.getPreferredStaffId())
                    .recommendedSlotsJson(recSlotsJson)
                    .weightsUsedJson(weightsJson)
                    .build();

            return logRepository.save(schedulingLog);
        } catch (Exception e) {
            log.error("Không thể ghi log SmartSchedulingLog", e);
            return null;
        }
    }

    private SmartSchedulingResponse buildEmptyResponse(SmartSchedulingRequest request, SmartSchedulingConfig config) {
        SmartSchedulingConfigDto configDto = SmartSchedulingConfigDto.builder()
                .id(config.getId())
                .branchId(config.getBranchId())
                .workloadWeight(config.getWorkloadWeight())
                .travelWeight(config.getTravelWeight())
                .serviceFitWeight(config.getServiceFitWeight())
                .description(config.getDescription())
                .build();

        return SmartSchedulingResponse.builder()
                .logId(null)
                .branchId(request.getBranchId())
                .date(request.getDate().toString())
                .weightsUsed(configDto)
                .recommendations(new ArrayList<>())
                .build();
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class SlotCandidate {
        private final double score;
        private final SlotRecommendationDto dto;
    }
}
