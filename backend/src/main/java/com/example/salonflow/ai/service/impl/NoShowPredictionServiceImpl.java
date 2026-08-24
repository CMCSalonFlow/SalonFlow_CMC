package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.dto.noshow.*;
import com.example.salonflow.ai.service.NoShowPredictionService;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.EmailService;
import com.example.salonflow.services.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoShowPredictionServiceImpl implements NoShowPredictionService {

    private final NoShowPredictionRepository predictionRepository;
    private final NoShowModelConfigRepository configRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SubscriptionService subscriptionService;
    private final BranchRepository branchRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public NoShowPredictionDto predictAndSaveLog(Booking booking) {
        log.info("Bắt đầu AI dự đoán No-Show cho Booking ID: {}", booking.getId());

        NoShowModelConfig config = getModelConfig();
        User customer = booking.getCustomer();
        Branch branch = booking.getBranch();

        // 1. Feature Engineering
        NoShowFeaturesDto features = extractFeatures(booking, customer, branch);

        // 2. Tính điểm z = beta0 + beta1*cancelRate + beta2*distanceNorm + beta3*leadTimeNorm - beta4*completedCountNorm
        double z = config.getBeta0().doubleValue()
                + (config.getBeta1().doubleValue() * features.getCancelRate())
                + (config.getBeta2().doubleValue() * features.getDistanceNorm())
                + (config.getBeta3().doubleValue() * features.getLeadTimeNorm())
                - (config.getBeta4().doubleValue() * features.getCompletedCountNorm());

        // 3. Hàm Sigmoid tính P(no-show)
        double probability = 1.0 / (1.0 + Math.exp(-z));
        double probabilityPercentage = Math.round(probability * 100.0 * 10.0) / 10.0;

        // 4. Phân loại Risk Level
        double threshold = config.getRiskThreshold().doubleValue();
        String riskLevel = "LOW";
        if (probability >= threshold) {
            riskLevel = "HIGH";
        } else if (probability >= 0.40) {
            riskLevel = "MEDIUM";
        }

        boolean isWarning = "HIGH".equals(riskLevel);

        // 5. Tạo mô tả lý do (Explanation)
        String explanation = buildExplanation(features, probabilityPercentage, riskLevel);

        // 6. Kiểm tra & Lưu/Cập nhật Log
        NoShowPredictionLog predictionLog = predictionRepository.findByBookingId(booking.getId())
                .orElseGet(() -> NoShowPredictionLog.builder()
                        .bookingId(booking.getId())
                        .customerId(customer.getId())
                        .branchId(branch.getId())
                        .build());

        predictionLog.setProbability(Math.round(probability * 1000.0) / 1000.0);
        predictionLog.setRiskLevel(riskLevel);
        predictionLog.setIsWarningTriggered(isWarning);
        predictionLog.setExplanation(explanation);

        try {
            predictionLog.setFeaturesJson(objectMapper.writeValueAsString(features));
        } catch (Exception e) {
            log.error("Không thể Serialize features Json", e);
        }

        // 7. Auto Trigger gửi Email nếu Nguy cơ cao & Cấu hình cho phép & chưa gửi trước đó
        if (isWarning && Boolean.TRUE.equals(config.getAutoSendReminder()) && !Boolean.TRUE.equals(predictionLog.getSmsSent())) {
            boolean sent = sendReminderToCustomer(booking, customer);
            if (sent) {
                predictionLog.setSmsSent(true);
                predictionLog.setSmsSentAt(LocalDateTime.now());
                log.info("Đã tự động gửi Email nhắc nhở cho Booking nguy cơ cao ID: {}", booking.getId());
            }
        }

        NoShowPredictionLog saved = predictionRepository.save(predictionLog);

        return mapToDto(saved, booking, customer, features);
    }

    @Override
    @Transactional(readOnly = true)
    public NoShowPredictionDto getPredictionByBookingId(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Booking với ID: " + bookingId));

        subscriptionService.validateAiFeatures(booking.getBranch().getSalon().getId());

        NoShowPredictionLog predictionLog = predictionRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    // Nếu chưa có log dự đoán, thực hiện dự đoán realtime
                    return saveRealtimePredictionLog(booking);
                });

        NoShowFeaturesDto features = parseFeaturesJson(predictionLog.getFeaturesJson());
        return mapToDto(predictionLog, booking, booking.getCustomer(), features);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NoShowPredictionDto> getHighRiskBookings(Long branchId, Pageable pageable) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh với ID: " + branchId));
        subscriptionService.validateAiFeatures(branch.getSalon().getId());

        Page<NoShowPredictionLog> page = predictionRepository.findByBranchIdAndRiskLevelOrderByCreatedAtDesc(branchId, "HIGH", pageable);
        return page.map(this::toDtoWithBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NoShowPredictionDto> getPredictionLogs(Long branchId, Pageable pageable) {
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh với ID: " + branchId));
            subscriptionService.validateAiFeatures(branch.getSalon().getId());
        }

        Page<NoShowPredictionLog> page = branchId != null
                ? predictionRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable)
                : predictionRepository.findAll(pageable);
        return page.map(this::toDtoWithBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public NoShowModelConfig getModelConfig() {
        return configRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> configRepository.save(NoShowModelConfig.builder()
                        .beta0(new BigDecimal("-1.500"))
                        .beta1(new BigDecimal("2.500"))
                        .beta2(new BigDecimal("1.200"))
                        .beta3(new BigDecimal("1.000"))
                        .beta4(new BigDecimal("2.000"))
                        .riskThreshold(new BigDecimal("0.700"))
                        .autoSendReminder(true)
                        .description("Cấu hình mặc định Logistic Regression AI No-Show")
                        .build()));
    }

    @Override
    @Transactional
    public NoShowModelConfig updateModelConfig(UpdateNoShowModelConfigDto dto) {
        NoShowModelConfig config = getModelConfig();

        if (dto.getBeta0() != null) config.setBeta0(dto.getBeta0());
        if (dto.getBeta1() != null) config.setBeta1(dto.getBeta1());
        if (dto.getBeta2() != null) config.setBeta2(dto.getBeta2());
        if (dto.getBeta3() != null) config.setBeta3(dto.getBeta3());
        if (dto.getBeta4() != null) config.setBeta4(dto.getBeta4());
        if (dto.getRiskThreshold() != null) config.setRiskThreshold(dto.getRiskThreshold());
        if (dto.getAutoSendReminder() != null) config.setAutoSendReminder(dto.getAutoSendReminder());
        if (dto.getDescription() != null) config.setDescription(dto.getDescription());

        return configRepository.save(config);
    }

    @Override
    @Transactional
    public boolean sendManualReminder(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Booking với ID: " + bookingId));

        subscriptionService.validateAiFeatures(booking.getBranch().getSalon().getId());

        boolean sent = sendReminderToCustomer(booking, booking.getCustomer());
        if (sent) {
            Optional<NoShowPredictionLog> logOpt = predictionRepository.findByBookingId(bookingId);
            if (logOpt.isPresent()) {
                NoShowPredictionLog pLog = logOpt.get();
                pLog.setSmsSent(true);
                pLog.setSmsSentAt(LocalDateTime.now());
                predictionRepository.save(pLog);
            }
        }
        return sent;
    }

    private boolean sendReminderToCustomer(Booking booking, User customer) {
        boolean emailSent = false;
        if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
            try {
                String subject = "🔔 [SalonFlow] Nhắc Nhở Lịch Hẹn Sắp Tới Tại Salon";
                String customerName = customer.getFullName() != null ? customer.getFullName() : (customer.getUsername() != null ? customer.getUsername() : "Quý khách");
                String branchName = booking.getBranch() != null ? booking.getBranch().getName() : "Salon";
                String dateStr = booking.getBookingDate() != null ? booking.getBookingDate().toString() : "";
                String startTimeStr = booking.getStartTime() != null ? booking.getStartTime().toString() : "";
                String timeStr = (startTimeStr + " " + dateStr).trim();

                String body = String.format(
                    "Xin chào %s,\n\n" +
                    "SalonFlow xin gửi thông báo nhắc nhở về lịch hẹn sắp tới của quý khách:\n" +
                    "- Mã Booking: #BK-%d\n" +
                    "- Chi nhánh: %s\n" +
                    "- Thời gian: %s\n\n" +
                    "Nếu quý khách có thay đổi lịch trình, vui lòng truy cập ứng dụng SalonFlow hoặc liên hệ Salon để xác nhận/hủy lịch sớm.\n\n" +
                    "Trân trọng,\nĐội ngũ SalonFlow",
                    customerName, booking.getId(), branchName, timeStr
                );

                emailService.sendNotificationEmail(customer.getEmail(), subject, body);
                emailSent = true;
                log.info("Đã gửi Email nhắc nhở thành công tới {}", customer.getEmail());
            } catch (Exception e) {
                log.error("Lỗi khi gửi Email nhắc nhở:", e);
            }
        }

        return emailSent;
    }

    // --- PRIVATE HELPER METHODS ---

    private NoShowFeaturesDto extractFeatures(Booking booking, User customer, Branch branch) {
        // 1. History Cancel Rate & Completed Count
        List<Booking> pastBookings = bookingRepository.findByCustomerId(customer.getId());
        long totalPast = 0;
        long totalCancelledNoShow = 0;
        long completedCount = 0;

        for (Booking b : pastBookings) {
            if (b.getId().equals(booking.getId())) continue; // Bỏ qua chính booking đang xét
            totalPast++;
            if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.NO_SHOW) {
                totalCancelledNoShow++;
            } else if (b.getStatus() == BookingStatus.COMPLETED) {
                completedCount++;
            }
        }

        double cancelRate = totalPast > 0
                ? (double) totalCancelledNoShow / totalPast
                : 0.15; // Mặc định baseline 15% cho khách mới

        double completedCountNorm = Math.min(1.0, (double) completedCount / 10.0);

        // 2. Distance Calculation
        double distanceKm = calculateDistanceKm(branch, customer);
        double distanceNorm = Math.min(1.0, distanceKm / 30.0); // 30km scale max

        // 3. Lead Time Calculation
        LocalDateTime bookingDateTime = booking.getBookingDate().atTime(booking.getStartTime());
        Instant createdInstant = booking.getCreatedAt() != null ? booking.getCreatedAt() : Instant.now();
        LocalDateTime createdDateTime = LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault());

        long leadMinutes = Math.max(0, Duration.between(createdDateTime, bookingDateTime).toMinutes());
        double leadTimeHours = (double) leadMinutes / 60.0;
        double leadTimeNorm = Math.min(1.0, leadTimeHours / 168.0); // 7 ngày = 168h scale max

        return NoShowFeaturesDto.builder()
                .cancelRate(Math.round(cancelRate * 100.0) / 100.0)
                .totalPastBookings(totalPast)
                .totalCancelledOrNoShowBookings(totalCancelledNoShow)
                .distanceKm(Math.round(distanceKm * 10.0) / 10.0)
                .distanceNorm(Math.round(distanceNorm * 100.0) / 100.0)
                .leadTimeHours(Math.round(leadTimeHours * 10.0) / 10.0)
                .leadTimeNorm(Math.round(leadTimeNorm * 100.0) / 100.0)
                .completedCount(completedCount)
                .completedCountNorm(Math.round(completedCountNorm * 100.0) / 100.0)
                .build();
    }

    private double calculateDistanceKm(Branch branch, User customer) {
        if (branch.getLatitude() == null || branch.getLongitude() == null) {
            return 5.0; // Mặc định 5km nếu chưa có tọa độ branch
        }
        // Giả lập hoặc lấy tọa độ khách hàng (nếu chưa có lat/lng trên user profile thì mặc định khoảng cách an toàn ~ 5km)
        double custLat = branch.getLatitude() + 0.04;
        double custLng = branch.getLongitude() + 0.04;

        double dLat = Math.toRadians(branch.getLatitude() - custLat);
        double dLng = Math.toRadians(branch.getLongitude() - custLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(custLat)) * Math.cos(Math.toRadians(branch.getLatitude())) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }

    private String buildExplanation(NoShowFeaturesDto f, double probPct, String riskLevel) {
        List<String> reasons = new ArrayList<>();

        if (f.getCancelRate() >= 0.3) {
            reasons.add("Tỉ lệ hủy/không đến trong quá khứ cao (" + Math.round(f.getCancelRate() * 100) + "%)");
        }
        if (f.getLeadTimeHours() >= 72) {
            reasons.add("Đặt lịch trước thời gian quá xa (" + Math.round(f.getLeadTimeHours() / 24) + " ngày)");
        }
        if (f.getDistanceKm() >= 15) {
            reasons.add("Khoảng cách vị trí xa chi nhánh (~" + f.getDistanceKm() + " km)");
        }
        if (f.getCompletedCount() >= 5) {
            reasons.add("Khách quen uy tín (Đã hoàn thành " + f.getCompletedCount() + " buổi dịch vụ)");
        }

        if (reasons.isEmpty()) {
            return "Xác suất No-Show dự đoán là " + probPct + "% dựa trên phân tích tổng hợp các chỉ số đặt lịch.";
        }

        return "Dự đoán nguy cơ " + riskLevel + " (" + probPct + "%): " + String.join(", ", reasons) + ".";
    }

    private NoShowPredictionLog saveRealtimePredictionLog(Booking booking) {
        NoShowPredictionDto dto = predictAndSaveLog(booking);
        return predictionRepository.findByBookingId(booking.getId()).orElse(null);
    }

    private NoShowFeaturesDto parseFeaturesJson(String json) {
        if (json == null || json.isEmpty()) return new NoShowFeaturesDto();
        try {
            return objectMapper.readValue(json, NoShowFeaturesDto.class);
        } catch (Exception e) {
            return new NoShowFeaturesDto();
        }
    }

    private NoShowPredictionDto mapToDto(NoShowPredictionLog pLog, Booking booking, User customer, NoShowFeaturesDto features) {
        double prob = pLog.getProbability() != null ? pLog.getProbability() : 0.0;
        double pct = Math.round(prob * 100.0 * 10.0) / 10.0;

        return NoShowPredictionDto.builder()
                .logId(pLog.getId())
                .bookingId(booking.getId())
                .customerId(customer.getId())
                .customerName(customer.getFullName() != null ? customer.getFullName() : "Khách hàng")
                .customerPhone(customer.getPhone() != null ? customer.getPhone() : "")
                .branchId(booking.getBranch().getId())
                .probability(prob)
                .probabilityPercentage(pct)
                .riskLevel(pLog.getRiskLevel())
                .features(features)
                .explanation(pLog.getExplanation())
                .isWarningTriggered(pLog.getIsWarningTriggered())
                .smsSent(pLog.getSmsSent())
                .smsSentAt(pLog.getSmsSentAt())
                .createdAt(pLog.getCreatedAt() != null ? pLog.getCreatedAt().toString() : Instant.now().toString())
                .build();
    }

    private NoShowPredictionDto toDtoWithBooking(NoShowPredictionLog pLog) {
        Booking booking = bookingRepository.findById(pLog.getBookingId()).orElse(null);
        if (booking == null) return null;
        NoShowFeaturesDto features = parseFeaturesJson(pLog.getFeaturesJson());
        return mapToDto(pLog, booking, booking.getCustomer(), features);
    }
}
