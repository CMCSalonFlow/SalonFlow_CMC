package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.dto.scheduling.SlotRecommendationDto;
import com.example.salonflow.ai.dto.scheduling.SmartSchedulingRequestDto;
import com.example.salonflow.ai.dto.scheduling.UpdateSmartSchedulingConfigDto;
import com.example.salonflow.ai.service.SmartSchedulingService;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.UserStatus;
import com.example.salonflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class SmartSchedulingServiceImpl implements SmartSchedulingService {

    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;
    private final BranchHourRepository branchHourRepository;
    private final ServiceRepository serviceRepository;
    private final SmartSchedulingLogRepository smartSchedulingLogRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Config weights default: 0.4 workload, 0.3 travel/gap, 0.3 service fit
    private BigDecimal workloadWeight = new BigDecimal("0.4");
    private BigDecimal travelWeight = new BigDecimal("0.3");
    private BigDecimal serviceFitWeight = new BigDecimal("0.3");

    @Override
    @Transactional
    public List<SlotRecommendationDto> recommendTopSlots(SmartSchedulingRequestDto request) {
        if (request == null || request.getBranchId() == null) {
            return Collections.emptyList();
        }

        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();

        // 1. Get branch working hours for date
        int dbDayOfWeek = date.getDayOfWeek().getValue() == 7 ? 0 : date.getDayOfWeek().getValue();
        Optional<BranchHour> branchHourOpt = branchHourRepository.findByBranchIdAndDayOfWeek(request.getBranchId(), dbDayOfWeek);
        
        LocalTime openTime = LocalTime.of(8, 0);
        LocalTime closeTime = LocalTime.of(20, 0);

        if (branchHourOpt.isPresent()) {
            if (Boolean.TRUE.equals(branchHourOpt.get().getIsClosed())) {
                return Collections.emptyList();
            }
            if (branchHourOpt.get().getOpenTime() != null) openTime = branchHourOpt.get().getOpenTime();
            if (branchHourOpt.get().getCloseTime() != null) closeTime = branchHourOpt.get().getCloseTime();
        }

        // 2. Calculate service duration
        int totalDuration = 30;
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<SalonService> services = serviceRepository.findAllById(request.getServiceIds());
            int sum = services.stream().mapToInt(SalonService::getDurationMinutes).sum();
            if (sum > 0) totalDuration = sum;
        }

        // 3. Filter staff list (Exclude Managers / Branch Managers / Owners and Inactive users)
        List<Staff> staffList = staffRepository.findByBranchId(request.getBranchId());
        if (request.getPreferredStaffId() != null) {
            staffList = staffList.stream()
                    .filter(s -> s.getId().equals(request.getPreferredStaffId()))
                    .collect(Collectors.toList());
        }

        List<Staff> qualifiedTechnicians = new ArrayList<>();
        for (Staff s : staffList) {
            if (s.getUserId() != null) {
                Optional<User> userOpt = userRepository.findByIdWithRoles(s.getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getStatus() != null && user.getStatus() != UserStatus.ACTIVE) {
                        continue; // Skip inactive staff
                    }
                    boolean isManagerOrOwner = user.getUserRoles() != null && user.getUserRoles().stream()
                            .anyMatch(ur -> ur.getRole() != null && (
                                    "MANAGER".equalsIgnoreCase(ur.getRole().getCode()) ||
                                    "BRANCH_MANAGER".equalsIgnoreCase(ur.getRole().getCode()) ||
                                    "SALON_OWNER".equalsIgnoreCase(ur.getRole().getCode()) ||
                                    "SYSTEM_ADMIN".equalsIgnoreCase(ur.getRole().getCode()) ||
                                    (ur.getRole().getName() != null && ur.getRole().getName().toLowerCase().contains("quản lý"))
                            ));
                    if (isManagerOrOwner && request.getPreferredStaffId() == null) {
                        // Skip managers from general AI appointment slot recommendations
                        continue;
                    }
                }
            }
            qualifiedTechnicians.add(s);
        }
        staffList = qualifiedTechnicians;

        if (staffList.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch all bookings for the branch on that date
        List<Booking> dayBookings = bookingRepository.findByBranchIdAndBookingDateBetween(request.getBranchId(), date, date);

        // Pre-calculate branch hourly occupancy (Peak-hour statistics)
        Map<Integer, Long> hourlyBookingCount = new HashMap<>();
        for (Booking b : dayBookings) {
            if (b.getStatus() != null && b.getStatus().name().equals("CANCELLED")) continue;
            if (b.getStartTime() != null) {
                int startHour = b.getStartTime().getHour();
                hourlyBookingCount.put(startHour, hourlyBookingCount.getOrDefault(startHour, 0L) + 1);
            }
        }
        int totalStaffCount = Math.max(1, staffList.size());

        List<SlotRecommendationDto> candidateSlots = new ArrayList<>();

        double wWorkload = workloadWeight.doubleValue(); // 0.4
        double wTravel = travelWeight.doubleValue();     // 0.3 (Occupancy / Peak-hour Optimization)
        double wServiceFit = serviceFitWeight.doubleValue(); // 0.3

        // Fetch selected service names & extract key skill tokens
        List<String> selectedServiceNames = new ArrayList<>();
        List<String> requiredSkillKeywords = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<SalonService> reqServices = serviceRepository.findAllById(request.getServiceIds());
            for (SalonService s : reqServices) {
                if (s.getName() != null) {
                    selectedServiceNames.add(s.getName());
                    String nameLower = s.getName().toLowerCase();
                    if (nameLower.contains("cắt")) requiredSkillKeywords.add("cắt");
                    if (nameLower.contains("uốn")) requiredSkillKeywords.add("uốn");
                    if (nameLower.contains("nhuộm")) requiredSkillKeywords.add("nhuộm");
                    if (nameLower.contains("gội")) requiredSkillKeywords.add("gội");
                    if (nameLower.contains("râu") || nameLower.contains("da mặt")) requiredSkillKeywords.add("râu");
                    if (nameLower.contains("massage")) requiredSkillKeywords.add("massage");
                    if (nameLower.contains("tạo kiểu") || nameLower.contains("barber")) requiredSkillKeywords.add("tạo kiểu");
                }
            }
        }

        for (Staff staff : staffList) {
            String staffSpecialtiesStr = staff.getSpecialties() != null ? staff.getSpecialties().toLowerCase() : "";

            // Rule-based Permitted Services & Skill Qualification Filter:
            // 1. Primary: Check explicit DB mapping staff.getServices() (table staff_services)
            // 2. Secondary: Fall back to specialty keyword matching
            boolean isQualified = true;

            if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
                Set<Long> reqServiceIds = new HashSet<>(request.getServiceIds());

                if (staff.getServices() != null && !staff.getServices().isEmpty()) {
                    Set<Long> staffServiceIds = staff.getServices().stream()
                            .map(SalonService::getId)
                            .collect(Collectors.toSet());

                    boolean canPerformServices = staffServiceIds.containsAll(reqServiceIds);
                    if (!canPerformServices) {
                        isQualified = false;
                    }
                } else if (!requiredSkillKeywords.isEmpty()) {
                    if (staffSpecialtiesStr.isEmpty()) {
                        isQualified = false;
                    } else {
                        boolean hasAnyMatch = false;
                        for (String kw : requiredSkillKeywords) {
                            if (staffSpecialtiesStr.contains(kw)) {
                                hasAnyMatch = true;
                                break;
                            }
                        }
                        if (!hasAnyMatch) {
                            isQualified = false;
                        }
                    }
                }
            }

            // Exclude unqualified staff from candidate recommendations
            if (!isQualified && request.getPreferredStaffId() == null) {
                continue;
            }

            List<Booking> staffBookings = dayBookings.stream()
                    .filter(b -> b.getAssignedStaff() != null && b.getAssignedStaff().getId().equals(staff.getId()))
                    .filter(b -> b.getStatus() != null && !b.getStatus().name().equals("CANCELLED"))
                    .sorted(Comparator.comparing(Booking::getStartTime))
                    .collect(Collectors.toList());

            // Component 1: staff_workload_balance (0.0 to 1.0)
            int staffBookedMinutes = staffBookings.stream().mapToInt(b -> {
                if (b.getTotalDurationMinutes() != null) return b.getTotalDurationMinutes();
                if (b.getStartTime() != null && b.getEndTime() != null) {
                    return (int) Duration.between(b.getStartTime(), b.getEndTime()).toMinutes();
                }
                return 30;
            }).sum();
            double staffWorkloadBalance = Math.max(0.1, 1.0 - Math.min(1.0, staffBookedMinutes / 480.0));

            // Component 3: service_fit based ONLY on Customer Rating ⭐ (60%) and Completed Bookings (40%)
            Double avgRatingObj = reviewRepository.findAverageRatingByStaffId(staff.getId());
            double avgRating = avgRatingObj != null ? avgRatingObj : 4.8;
            double ratingScore = Math.min(1.0, avgRating / 5.0);

            long completedBookings = bookingRepository.countByAssignedStaffIdAndStatus(staff.getId(), com.example.salonflow.entity.enums.BookingStatus.COMPLETED);
            double experienceScore = Math.min(1.0, 0.50 + (completedBookings / 100.0));

            double serviceFit = (ratingScore * 0.60) + (experienceScore * 0.40);
            if (request.getPreferredStaffId() != null && request.getPreferredStaffId().equals(staff.getId())) {
                serviceFit = 1.0;
            }

            // Loop candidate time slots every 30 mins
            LocalTime currentTime = openTime;
            LocalTime nowTime = LocalTime.now();
            boolean isToday = date.equals(LocalDate.now());

            while (!currentTime.plusMinutes(totalDuration).isAfter(closeTime)) {
                LocalTime slotStart = currentTime;
                LocalTime slotEnd = slotStart.plusMinutes(totalDuration);

                // Rule-based Filter 1: Filter out past slots if date is today
                if (isToday && slotStart.isBefore(nowTime)) {
                    currentTime = currentTime.plusMinutes(30);
                    continue;
                }

                // Rule-based Filter 2: Check slot conflict with staff's existing bookings
                boolean hasOverlap = staffBookings.stream().anyMatch(b ->
                        !(slotEnd.isBefore(b.getStartTime()) || slotEnd.equals(b.getStartTime()) ||
                          slotStart.isAfter(b.getEndTime()) || slotStart.equals(b.getEndTime()))
                );

                if (!hasOverlap) {
                    // Component 2: Occupancy / Peak-hour Optimization with Dynamic Weekend & Weekday Curve
                    int hour = slotStart.getHour();
                    int minute = slotStart.getMinute();
                    long bookingsInHour = hourlyBookingCount.getOrDefault(hour, 0L);
                    double occupancyRate = (double) bookingsInHour / (double) totalStaffCount;

                    boolean isWeekend = (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY);

                    // Dynamic Peak-hour Base Curve
                    double timeOfDayBase = 0.85;
                    if (isWeekend) {
                        // Cuối tuần (Thứ 7, CN): Đông cả ngày từ 09:00 - 16:30 -> Đẩy về đầu ca sáng 8h hoặc chiều tối
                        if (hour == 8) timeOfDayBase = minute == 0 ? 0.96 : 0.93;
                        else if (hour >= 9 && hour <= 16) timeOfDayBase = 0.60; // Giờ vàng cuối tuần
                        else if (hour >= 17) timeOfDayBase = minute == 0 ? 0.88 : 0.82;
                    } else {
                        // Ngày thường (Thứ 2 - Thứ 6): Vắng ban ngày, đông giờ tan làm 17:00 - 20:00
                        if (hour == 8) timeOfDayBase = minute == 0 ? 0.95 : 0.92;
                        else if (hour == 9) timeOfDayBase = minute == 0 ? 0.89 : 0.86;
                        else if (hour == 10) timeOfDayBase = minute == 0 ? 0.83 : 0.80;
                        else if (hour == 11) timeOfDayBase = minute == 0 ? 0.77 : 0.74;
                        else if (hour == 12) timeOfDayBase = 0.70; // Giờ nghỉ trưa
                        else if (hour == 13) timeOfDayBase = 0.75;
                        else if (hour == 14) timeOfDayBase = minute == 0 ? 0.92 : 0.89;
                        else if (hour == 15) timeOfDayBase = minute == 0 ? 0.86 : 0.83;
                        else if (hour == 16) timeOfDayBase = minute == 0 ? 0.80 : 0.77;
                        else if (hour >= 17) timeOfDayBase = 0.50; // Giờ tan làm ngày thường
                    }

                    // Trừ điểm theo Mật độ Lấp đầy Thực tế (Xử lý Ngày Lễ/Tết quá tải)
                    double occupancyScore = timeOfDayBase - (occupancyRate * 0.35);

                    // Shift continuity gap bonus (nối ca bận tiếp theo)
                    double gapBonus = 0.0;
                    if (!staffBookings.isEmpty()) {
                        long minGapMinutes = Long.MAX_VALUE;
                        for (Booking b : staffBookings) {
                            if (slotStart.isAfter(b.getEndTime())) {
                                minGapMinutes = Math.min(minGapMinutes, Duration.between(b.getEndTime(), slotStart).toMinutes());
                            }
                            if (b.getStartTime().isAfter(slotEnd)) {
                                minGapMinutes = Math.min(minGapMinutes, Duration.between(slotEnd, b.getStartTime()).toMinutes());
                            }
                        }
                        if (minGapMinutes == 0) gapBonus = 0.08;
                        else if (minGapMinutes <= 15) gapBonus = 0.04;
                    }

                    double occupancyPeakOptimization = Math.max(0.1, Math.min(1.0, occupancyScore + gapBonus));

                    // Combined score formula: score = (staff_workload_balance * 0.4 + occupancy_peak_optimization * 0.3 + serviceFit * 0.3)
                    double score = (staffWorkloadBalance * wWorkload) 
                                 + (occupancyPeakOptimization * wTravel) 
                                 + (serviceFit * wServiceFit);
                    
                    double totalScore = Math.round(score * 100.0 * 10.0) / 10.0;

                    String staffName = staff.getName() != null ? staff.getName() : "Thợ Salon";
                    String staffAvatar = staff.getAvatarUrl();

                    // Generate professional concise reason list
                    List<String> reasons = new ArrayList<>();
                    reasons.add(String.format("Đánh giá %.1f⭐ (%d ca phục vụ)", avgRating, completedBookings));
                    
                    if (isWeekend && hour >= 9 && hour <= 16) {
                        reasons.add("Giờ cao điểm cuối tuần");
                    } else if (!isWeekend && hour >= 17) {
                        reasons.add("Giờ cao điểm tan làm");
                    } else if (occupancyRate < 0.4) {
                        reasons.add("Khung giờ rảnh rỗi, phục vụ chu đáo");
                    } else {
                        reasons.add("Khung giờ khả dụng tiêu chuẩn");
                    }

                    candidateSlots.add(SlotRecommendationDto.builder()
                            .startTime(slotStart)
                            .endTime(slotEnd)
                            .staffId(staff.getId())
                            .staffName(staffName)
                            .staffAvatar(staffAvatar)
                            .staffSpecialties(staff.getSpecialties())
                            .totalScore(totalScore)
                            .workloadBalanceScore(Math.round(staffWorkloadBalance * 100.0) / 100.0)
                            .travelGapScore(Math.round(occupancyPeakOptimization * 100.0) / 100.0)
                            .occupancyScore(Math.round(occupancyPeakOptimization * 100.0) / 100.0)
                            .serviceFitScore(Math.round(serviceFit * 100.0) / 100.0)
                            .reasonList(reasons)
                            .build());
                }

                currentTime = currentTime.plusMinutes(30);
            }
        }

        // Sort descending by totalScore
        List<SlotRecommendationDto> sortedCandidates = candidateSlots.stream()
                .sorted(Comparator.comparing(SlotRecommendationDto::getTotalScore).reversed())
                .collect(Collectors.toList());

        List<SlotRecommendationDto> topSlots = new ArrayList<>();
        Set<Long> chosenStaffIds = new HashSet<>();
        Set<String> chosenTimeStaffPairs = new HashSet<>();

        if (request.getPreferredStaffId() != null) {
            // Specific preferred staff: Pick top 3 distinct time slots
            for (SlotRecommendationDto slot : sortedCandidates) {
                if (topSlots.size() >= 3) break;
                boolean timeAlreadyPicked = topSlots.stream().anyMatch(e -> e.getStartTime().equals(slot.getStartTime()));
                if (!timeAlreadyPicked) {
                    topSlots.add(slot);
                }
            }
        } else {
            // "Bất kỳ ai" (Auto): Pick top candidate options prioritizing distinct staff members
            // Pass 1: Select top slots for distinct staff members (allowing optimal time slot for each staff)
            for (SlotRecommendationDto slot : sortedCandidates) {
                if (topSlots.size() >= 3) break;
                String pairKey = slot.getStartTime() + "_" + slot.getStaffId();
                if (chosenTimeStaffPairs.contains(pairKey)) continue;

                boolean staffAlreadyChosen = chosenStaffIds.contains(slot.getStaffId());
                if (!staffAlreadyChosen || chosenStaffIds.size() >= staffList.size()) {
                    topSlots.add(slot);
                    chosenStaffIds.add(slot.getStaffId());
                    chosenTimeStaffPairs.add(pairKey);
                }
            }

            // Pass 2: Fallback if fewer than 3 slots picked
            if (topSlots.size() < 3) {
                for (SlotRecommendationDto slot : sortedCandidates) {
                    if (topSlots.size() >= 3) break;
                    String pairKey = slot.getStartTime() + "_" + slot.getStaffId();
                    if (!chosenTimeStaffPairs.contains(pairKey)) {
                        topSlots.add(slot);
                        chosenTimeStaffPairs.add(pairKey);
                    }
                }
            }
        }

        // Re-sort topSlots descending by totalScore
        topSlots.sort(Comparator.comparing(SlotRecommendationDto::getTotalScore).reversed());

        // Generate Vietnamese formal explanations for top slots
        for (int i = 0; i < topSlots.size(); i++) {
            SlotRecommendationDto slot = topSlots.get(i);
            String explanation = String.format("Khung giờ %s - %s: Đề xuất Nhân viên phục vụ %s (%s điểm).",
                    slot.getStartTime(),
                    slot.getEndTime(),
                    slot.getStaffName(),
                    slot.getTotalScore());
            slot.setExplanation(explanation);
        }

        // Automatically save log entry matching smart_schedule_logs DB table schema
        try {
            String serviceIdsStr = request.getServiceIds() != null
                    ? request.getServiceIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                    : null;
            String jsonSlots = objectMapper.writeValueAsString(topSlots);

            String topSlotTime = !topSlots.isEmpty() && topSlots.get(0).getStartTime() != null
                    ? topSlots.get(0).getStartTime().toString()
                    : null;

            SmartSchedulingLog logEntry = SmartSchedulingLog.builder()
                    .branchId(request.getBranchId())
                    .customerId(request.getCustomerId())
                    .requestDate(date)
                    .serviceIds(serviceIdsStr)
                    .staffId(request.getPreferredStaffId())
                    .recommendedSlotsJson(jsonSlots)
                    .selectedSlotTime(topSlotTime)
                    .isBooked(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            smartSchedulingLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to save smart scheduling log to DB: {}", ex.getMessage());
        }

        return topSlots;
    }

    @Override
    public UpdateSmartSchedulingConfigDto getConfig(Long branchId) {
        return UpdateSmartSchedulingConfigDto.builder()
                .workloadWeight(workloadWeight)
                .travelWeight(travelWeight)
                .serviceFitWeight(serviceFitWeight)
                .description("Trọng số thuật toán AI Smart Scheduling: Cân bằng tải (0.4) + Tối ưu thời gian chờ (0.3) + Độ phù hợp dịch vụ (0.3)")
                .build();
    }

    @Override
    public UpdateSmartSchedulingConfigDto updateConfig(Long branchId, UpdateSmartSchedulingConfigDto dto) {
        if (dto != null) {
            if (dto.getWorkloadWeight() != null) this.workloadWeight = dto.getWorkloadWeight();
            if (dto.getTravelWeight() != null) this.travelWeight = dto.getTravelWeight();
            if (dto.getServiceFitWeight() != null) this.serviceFitWeight = dto.getServiceFitWeight();
        }
        return getConfig(branchId);
    }

    @Override
    public List<SmartSchedulingLog> getLogs(Long branchId) {
        if (branchId != null) {
            return smartSchedulingLogRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        }
        return smartSchedulingLogRepository.findAllByOrderByCreatedAtDesc();
    }
}
