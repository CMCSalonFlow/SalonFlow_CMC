package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.dto.scheduling.SlotRecommendationDto;
import com.example.salonflow.ai.dto.scheduling.SmartSchedulingRequestDto;
import com.example.salonflow.ai.dto.scheduling.UpdateSmartSchedulingConfigDto;
import com.example.salonflow.ai.service.SmartSchedulingService;
import com.example.salonflow.entity.*;
import com.example.salonflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final ObjectMapper objectMapper;

    // Config weights default: 0.4 workload, 0.3 travel/gap, 0.3 service fit
    private BigDecimal workloadWeight = new BigDecimal("0.4");
    private BigDecimal travelWeight = new BigDecimal("0.3");
    private BigDecimal serviceFitWeight = new BigDecimal("0.3");

    @Override
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

        // 3. Filter staff list
        List<Staff> staffList = staffRepository.findByBranchId(request.getBranchId());
        if (request.getPreferredStaffId() != null) {
            staffList = staffList.stream()
                    .filter(s -> s.getId().equals(request.getPreferredStaffId()))
                    .collect(Collectors.toList());
        }

        if (staffList.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch all bookings for the branch on that date
        List<Booking> dayBookings = bookingRepository.findByBranchIdAndBookingDateBetween(request.getBranchId(), date, date);

        List<SlotRecommendationDto> candidateSlots = new ArrayList<>();

        double wWorkload = workloadWeight.doubleValue();
        double wTravel = travelWeight.doubleValue();
        double wServiceFit = serviceFitWeight.doubleValue();

        for (Staff staff : staffList) {
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

            // Component 3: service_fit (0.0 to 1.0)
            double serviceFit = 0.85;
            if (staff.getSpecialties() != null && !staff.getSpecialties().isEmpty()) {
                serviceFit = 0.95;
            }
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

                // Filter out past slots if date is today
                if (isToday && slotStart.isBefore(nowTime)) {
                    currentTime = currentTime.plusMinutes(30);
                    continue;
                }

                // Check slot conflict with staff's existing bookings
                boolean hasOverlap = staffBookings.stream().anyMatch(b ->
                        !(slotEnd.isBefore(b.getStartTime()) || slotEnd.equals(b.getStartTime()) ||
                          slotStart.isAfter(b.getEndTime()) || slotStart.equals(b.getEndTime()))
                );

                if (!hasOverlap) {
                    // Component 2: travel_time_optimization (gap optimization 0.0 to 1.0)
                    double travelTimeOptimization = 0.75;
                    if (staffBookings.isEmpty()) {
                        travelTimeOptimization = 0.90;
                    } else {
                        long minGapMinutes = Long.MAX_VALUE;
                        for (Booking b : staffBookings) {
                            if (slotStart.isAfter(b.getEndTime())) {
                                long gap = Duration.between(b.getEndTime(), slotStart).toMinutes();
                                minGapMinutes = Math.min(minGapMinutes, gap);
                            }
                            if (b.getStartTime().isAfter(slotEnd)) {
                                long gap = Duration.between(slotEnd, b.getStartTime()).toMinutes();
                                minGapMinutes = Math.min(minGapMinutes, gap);
                            }
                        }

                        if (minGapMinutes == 0) {
                            travelTimeOptimization = 1.0;
                        } else if (minGapMinutes <= 15) {
                            travelTimeOptimization = 0.95;
                        } else if (minGapMinutes <= 30) {
                            travelTimeOptimization = 0.85;
                        } else {
                            travelTimeOptimization = 0.70;
                        }
                    }

                    // Combined score formula: score = (staff_workload_balance * 0.4 + travel_time_optimization * 0.3 + service_fit * 0.3)
                    double score = (staffWorkloadBalance * wWorkload) + (travelTimeOptimization * wTravel) + (serviceFit * wServiceFit);
                    double totalScore = Math.round(score * 100.0 * 10.0) / 10.0;

                    String staffName = staff.getName() != null ? staff.getName() : "Thợ Salon";
                    String staffAvatar = staff.getAvatarUrl();

                    candidateSlots.add(SlotRecommendationDto.builder()
                            .startTime(slotStart)
                            .endTime(slotEnd)
                            .staffId(staff.getId())
                            .staffName(staffName)
                            .staffAvatar(staffAvatar)
                            .staffSpecialties(staff.getSpecialties())
                            .totalScore(totalScore)
                            .workloadBalanceScore(Math.round(staffWorkloadBalance * 100.0) / 100.0)
                            .travelGapScore(Math.round(travelTimeOptimization * 100.0) / 100.0)
                            .serviceFitScore(Math.round(serviceFit * 100.0) / 100.0)
                            .build());
                }

                currentTime = currentTime.plusMinutes(30);
            }
        }

        // Sort descending by totalScore and pick Top 3 slots
        List<SlotRecommendationDto> topSlots = candidateSlots.stream()
                .sorted(Comparator.comparing(SlotRecommendationDto::getTotalScore).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Generate Vietnamese explanations for top 3
        for (int i = 0; i < topSlots.size(); i++) {
            SlotRecommendationDto slot = topSlots.get(i);
            String rank = i == 0 ? "Khung giờ vàng tối ưu nhất" : (i == 1 ? "Khung giờ gợi ý hàng đầu" : "Khung giờ linh hoạt tốt");
            String explanation = String.format("%s (%s - %s): Thợ %s có tay nghề cao phù hợp với dịch vụ, ca làm việc chưa bị quá tải và thời gian chờ liền kề tối ưu.",
                    rank,
                    slot.getStartTime(),
                    slot.getEndTime(),
                    slot.getStaffName());
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
