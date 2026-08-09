package com.example.salonflow.services.impl;

import com.example.salonflow.ai.dto.campaign.AiCampaignSuggestionRequest;
import com.example.salonflow.ai.dto.campaign.AiCampaignSuggestionResponse;
import com.example.salonflow.dto.analytics.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.DiscountType;
import com.example.salonflow.entity.enums.NotificationChannel;
import com.example.salonflow.entity.enums.NotificationStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.CustomerAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.example.salonflow.services.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAnalyticsServiceImpl implements CustomerAnalyticsService {

    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final VoucherRepository voucherRepository;
    private final TargetedCampaignRepository targetedCampaignRepository;
    private final EmailService emailService;

    @Override
    public CustomerSegmentationOverviewResponse getCustomerSegmentationOverview(Long branchId) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        String branchName = "Tất cả chi nhánh";
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh ID: " + branchId));
            if (!branch.getSalon().getId().equals(salon.getId())) {
                throw new IllegalArgumentException("Chi nhánh không thuộc quyền quản lý của Salon này");
            }
            branchName = branch.getName();
        }

        List<CustomerSegmentDetailDto> allCustomers = buildCustomerMetricsList(salon.getId(), branchId);

        long totalCount = allCustomers.size();
        long newCount = allCustomers.stream().filter(c -> "NEW".equalsIgnoreCase(c.getSegmentType())).count();
        long returningCount = allCustomers.stream().filter(c -> "RETURNING".equalsIgnoreCase(c.getSegmentType())).count();
        long vipCount = allCustomers.stream().filter(c -> "VIP".equalsIgnoreCase(c.getSegmentType())).count();
        long atRiskCount = allCustomers.stream().filter(c -> "AT_RISK".equalsIgnoreCase(c.getSegmentType())).count();

        double newPct = totalCount > 0 ? Math.round(((double) newCount / totalCount * 100.0) * 10.0) / 10.0 : 0.0;
        double returningPct = totalCount > 0 ? Math.round(((double) returningCount / totalCount * 100.0) * 10.0) / 10.0 : 0.0;
        double vipPct = totalCount > 0 ? Math.round(((double) vipCount / totalCount * 100.0) * 10.0) / 10.0 : 0.0;
        double atRiskPct = totalCount > 0 ? Math.round(((double) atRiskCount / totalCount * 100.0) * 10.0) / 10.0 : 0.0;

        BigDecimal totalRevenue = allCustomers.stream()
                .map(CustomerSegmentDetailDto::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCompletedBookings = allCustomers.stream()
                .mapToLong(CustomerSegmentDetailDto::getCompletedBookingsCount)
                .sum();

        BigDecimal avgAov = totalCompletedBookings > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalCompletedBookings), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double avgFreq = allCustomers.stream()
                .mapToDouble(CustomerSegmentDetailDto::getFrequencyPerMonth)
                .average()
                .orElse(0.0);
        avgFreq = Math.round(avgFreq * 10.0) / 10.0;

        BigDecimal avgClv = avgAov.multiply(BigDecimal.valueOf(avgFreq)).multiply(BigDecimal.valueOf(12))
                .setScale(2, RoundingMode.HALF_UP);

        return CustomerSegmentationOverviewResponse.builder()
                .salonId(salon.getId())
                .branchId(branchId)
                .branchName(branchName)
                .totalCustomers(totalCount)
                .newCount(newCount)
                .newPercentage(newPct)
                .returningCount(returningCount)
                .returningPercentage(returningPct)
                .vipCount(vipCount)
                .vipPercentage(vipPct)
                .atRiskCount(atRiskCount)
                .atRiskPercentage(atRiskPct)
                .averageOrderValue(avgAov)
                .averageFrequencyPerMonth(avgFreq)
                .averageCustomerLifetimeValue(avgClv)
                .build();
    }

    @Override
    public CustomerFunnelAnalyticsResponse getCustomerConversionFunnel(Long branchId) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        List<Booking> allBookings = branchId != null
                ? bookingRepository.findByBranchId(branchId)
                : bookingRepository.findByBranchSalonId(salon.getId());

        long totalInteractedCustomers = allBookings.stream()
                .map(b -> b.getCustomer().getId())
                .distinct()
                .count();

        List<CustomerSegmentDetailDto> customers = buildCustomerMetricsList(salon.getId(), branchId);

        long stage1Count = totalInteractedCustomers;
        long stage2NewCount = customers.stream().filter(c -> c.getCompletedBookingsCount() >= 1).count();
        long stage3ReturningCount = customers.stream().filter(c -> c.getCompletedBookingsCount() >= 2).count();
        long stage4VipCount = customers.stream().filter(c -> c.getCompletedBookingsCount() > 5 || c.getTotalSpent().compareTo(BigDecimal.valueOf(5000000)) >= 0).count();
        long atRiskDropCount = customers.stream().filter(c -> "AT_RISK".equalsIgnoreCase(c.getSegmentType())).count();

        double s1Rate = 100.0;
        double s2Rate = stage1Count > 0 ? Math.round(((double) stage2NewCount / stage1Count * 100.0) * 10.0) / 10.0 : 0.0;
        double s3Rate = stage2NewCount > 0 ? Math.round(((double) stage3ReturningCount / stage2NewCount * 100.0) * 10.0) / 10.0 : 0.0;
        double s4Rate = stage3ReturningCount > 0 ? Math.round(((double) stage4VipCount / stage3ReturningCount * 100.0) * 10.0) / 10.0 : 0.0;
        double atRiskRate = stage2NewCount > 0 ? Math.round(((double) atRiskDropCount / stage2NewCount * 100.0) * 10.0) / 10.0 : 0.0;

        List<CustomerFunnelAnalyticsResponse.FunnelStageDto> stages = List.of(
                CustomerFunnelAnalyticsResponse.FunnelStageDto.builder()
                        .stageKey("TOTAL_INTERACTED")
                        .stageName("Tương tác / Đặt lịch")
                        .count(stage1Count)
                        .conversionRate(s1Rate)
                        .overallRate(100.0)
                        .build(),
                CustomerFunnelAnalyticsResponse.FunnelStageDto.builder()
                        .stageKey("NEW_CUSTOMER")
                        .stageName("Hoàn thành lần đầu (New)")
                        .count(stage2NewCount)
                        .conversionRate(s2Rate)
                        .overallRate(stage1Count > 0 ? Math.round(((double) stage2NewCount / stage1Count * 100.0) * 10.0) / 10.0 : 0.0)
                        .build(),
                CustomerFunnelAnalyticsResponse.FunnelStageDto.builder()
                        .stageKey("RETURNING_CUSTOMER")
                        .stageName("Khách quay lại (2-5 lần)")
                        .count(stage3ReturningCount)
                        .conversionRate(s3Rate)
                        .overallRate(stage1Count > 0 ? Math.round(((double) stage3ReturningCount / stage1Count * 100.0) * 10.0) / 10.0 : 0.0)
                        .build(),
                CustomerFunnelAnalyticsResponse.FunnelStageDto.builder()
                        .stageKey("VIP_CUSTOMER")
                        .stageName("Khách VIP (>5 lần / >5M)")
                        .count(stage4VipCount)
                        .conversionRate(s4Rate)
                        .overallRate(stage1Count > 0 ? Math.round(((double) stage4VipCount / stage1Count * 100.0) * 10.0) / 10.0 : 0.0)
                        .build(),
                CustomerFunnelAnalyticsResponse.FunnelStageDto.builder()
                        .stageKey("AT_RISK_DROP")
                        .stageName("Nguy cơ rời bỏ (>60 ngày)")
                        .count(atRiskDropCount)
                        .conversionRate(atRiskRate)
                        .overallRate(stage1Count > 0 ? Math.round(((double) atRiskDropCount / stage1Count * 100.0) * 10.0) / 10.0 : 0.0)
                        .build()
        );

        return CustomerFunnelAnalyticsResponse.builder()
                .salonId(salon.getId())
                .branchId(branchId)
                .stages(stages)
                .build();
    }

    @Override
    public Page<CustomerSegmentDetailDto> getCustomersBySegment(Long branchId, String segmentType, String searchQuery, Pageable pageable) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        List<CustomerSegmentDetailDto> list = buildCustomerMetricsList(salon.getId(), branchId);

        // Filter segment
        if (segmentType != null && !segmentType.isBlank() && !"ALL".equalsIgnoreCase(segmentType)) {
            list = list.stream()
                    .filter(c -> segmentType.equalsIgnoreCase(c.getSegmentType()))
                    .collect(Collectors.toList());
        }

        // Filter search query
        if (searchQuery != null && !searchQuery.isBlank()) {
            String q = searchQuery.toLowerCase().trim();
            list = list.stream()
                    .filter(c -> (c.getFullName() != null && c.getFullName().toLowerCase().contains(q)) ||
                                 (c.getPhone() != null && c.getPhone().contains(q)))
                    .collect(Collectors.toList());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());

        if (start > list.size()) {
            return new PageImpl<>(List.of(), pageable, list.size());
        }

        List<CustomerSegmentDetailDto> subList = list.subList(start, end);
        return new PageImpl<>(subList, pageable, list.size());
    }

    @Override
    public AiCampaignSuggestionResponse generateAiCampaignSuggestion(AiCampaignSuggestionRequest request) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        String salonName = salon.getName() != null ? salon.getName() : "Salon";
        String segment = request.getSegmentType() != null ? request.getSegmentType().toUpperCase() : "AT_RISK";

        List<CustomerSegmentDetailDto> segmentCustomers = buildCustomerMetricsList(salon.getId(), request.getBranchId())
                .stream()
                .filter(c -> segment.equalsIgnoreCase(c.getSegmentType()))
                .toList();

        long targetCount = segmentCustomers.size();

        return switch (segment) {
            case "AT_RISK" -> AiCampaignSuggestionResponse.builder()
                    .segmentType("AT_RISK")
                    .campaignName("Chiến dịch khôi phục Khách hàng ngưng quay lại (" + targetCount + " khách)")
                    .suggestedTitle(salonName + " rất nhớ bạn! Nhận ngay ưu đãi 20%")
                    .suggestedMessage("Chào bạn! Đã hơn 60 ngày rồi " + salonName + " chưa được phục vụ bạn. Nhập ngay mã ưu đãi dành riêng cho bạn để tận hưởng phút giây thư giãn và làm mới bản thân nhé!")
                    .discountType("PERCENTAGE")
                    .discountValue(BigDecimal.valueOf(20))
                    .minOrderAmount(BigDecimal.valueOf(200000))
                    .maxDiscountAmount(BigDecimal.valueOf(100000))
                    .strategyExplanation("Phân tích CSDL: Đã phát hiện " + targetCount + " khách hàng > 60 ngày chưa quay lại. Đề xuất ưu đãi 20% để kéo khách hàng tái kích hoạt.")
                    .build();
            case "VIP" -> AiCampaignSuggestionResponse.builder()
                    .segmentType("VIP")
                    .campaignName("Chiến dịch Tri ân Khách hàng VIP (" + targetCount + " khách)")
                    .suggestedTitle("Đặc quyền Tri ân Khách hàng VIP tại " + salonName)
                    .suggestedMessage("Trân trọng cảm ơn sự gắn kết đặc biệt của Quý khách! " + salonName + " dành tặng riêng Quý khách VIP voucher 150.000đ cho mọi dịch vụ chăm sóc cao cấp.")
                    .discountType("FIXED")
                    .discountValue(BigDecimal.valueOf(150000))
                    .minOrderAmount(BigDecimal.valueOf(500000))
                    .maxDiscountAmount(BigDecimal.valueOf(150000))
                    .strategyExplanation("Phân tích CSDL: Đã ghi nhận " + targetCount + " khách hàng đạt cấp VIP. Đề xuất Voucher tri ân 150.000đ để giữ chân khách hàng cao cấp lâu dài.")
                    .build();
            case "NEW" -> AiCampaignSuggestionResponse.builder()
                    .segmentType("NEW")
                    .campaignName("Chiến dịch Chào mừng Khách mới quay lại (" + targetCount + " khách)")
                    .suggestedTitle("Ưu đãi 15% cho lượt đặt lịch thứ 2 tại " + salonName)
                    .suggestedMessage("Cảm ơn bạn đã lựa chọn trải nghiệm dịch vụ tại " + salonName + "! Nhận ngay ưu đãi 15% cho lượt đặt lịch tiếp theo để trải nghiệm trọn vẹn sự chăm sóc chu đáo.")
                    .discountType("PERCENTAGE")
                    .discountValue(BigDecimal.valueOf(15))
                    .minOrderAmount(BigDecimal.valueOf(150000))
                    .maxDiscountAmount(BigDecimal.valueOf(75000))
                    .strategyExplanation("Phân tích CSDL: Hiện có " + targetCount + " khách mới vừa hoàn thành 1 dịch vụ. Đề xuất giảm 15% để khuyến khích đặt lần 2.")
                    .build();
            default -> AiCampaignSuggestionResponse.builder()
                    .segmentType("RETURNING")
                    .campaignName("Chiến dịch Nâng hạng VIP cho Khách quay lại (" + targetCount + " khách)")
                    .suggestedTitle("Ưu đãi 10% - Tích điểm nâng hạng VIP cùng " + salonName)
                    .suggestedMessage("Cảm ơn sự ủng hộ thường xuyên của bạn! Đặt lịch ngay hôm nay tại " + salonName + " để nhận thêm 10% giảm giá và tích lũy số lần đặt lịch nâng hạng VIP.")
                    .discountType("PERCENTAGE")
                    .discountValue(BigDecimal.valueOf(10))
                    .minOrderAmount(BigDecimal.valueOf(200000))
                    .maxDiscountAmount(BigDecimal.valueOf(50000))
                    .strategyExplanation("Phân tích CSDL: Đã có " + targetCount + " khách hàng đặt 2-5 lần. Đề xuất giảm 10% thúc đẩy họ tích điểm thành khách VIP.")
                    .build();
        };
    }

    @Override
    @Transactional
    public TargetedCampaign executeTargetedCampaign(TargetedCampaignCreateRequest request) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh ID: " + request.getBranchId()));
        }

        // 1. Tạo Voucher nếu được yêu cầu
        Voucher createdVoucher = null;
        if (Boolean.TRUE.equals(request.getCreateVoucher())) {
            String code = request.getVoucherCode();
            if (code == null || code.isBlank()) {
                code = "CAMP-" + request.getSegmentType() + "-" + (System.currentTimeMillis() % 100000);
            }

            DiscountType dType = "FIXED".equalsIgnoreCase(request.getDiscountType())
                    ? DiscountType.FIXED
                    : DiscountType.PERCENT;

            int validDays = request.getValidDays() != null ? request.getValidDays() : 14;

            createdVoucher = Voucher.builder()
                    .code(code)
                    .discountType(dType)
                    .discountValue(request.getDiscountValue() != null ? request.getDiscountValue() : BigDecimal.valueOf(10))
                    .minOrderAmount(request.getMinOrderAmount())
                    .maxDiscountAmount(request.getMaxDiscountAmount())
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(validDays))
                    .isActive(true)
                    .usageLimit(1000)
                    .usedCount(0)
                    .build();

            createdVoucher = voucherRepository.save(createdVoucher);
        }

        // 2. Lấy danh sách khách hàng thuộc Segment
        List<CustomerSegmentDetailDto> targetCustomers = buildCustomerMetricsList(salon.getId(), request.getBranchId())
                .stream()
                .filter(c -> request.getSegmentType().equalsIgnoreCase(c.getSegmentType()))
                .toList();

        // 3. Tạo thông báo hàng loạt cho danh sách khách hàng
        String finalMessage = request.getMessageContent();
        String formattedEndDate = "";
        if (createdVoucher != null) {
            if (createdVoucher.getEndDate() != null) {
                formattedEndDate = createdVoucher.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            finalMessage += "\n\nMã Voucher của bạn: " + createdVoucher.getCode();
            if (!formattedEndDate.isBlank()) {
                finalMessage += "\nHạn sử dụng: Đến hết ngày " + formattedEndDate;
            }
        }

        for (CustomerSegmentDetailDto customerDto : targetCustomers) {
            User recipient = userRepository.findById(customerDto.getCustomerId()).orElse(null);
            if (recipient != null) {
                // 1. Tạo thông báo In-App
                Notification notif = Notification.builder()
                        .recipient(recipient)
                        .channel(NotificationChannel.IN_APP)
                        .status(NotificationStatus.UNREAD)
                        .title(request.getMessageTitle())
                        .message(finalMessage)
                        .sourceType("TARGETED_CAMPAIGN")
                        .eventType("CAMPAIGN_PROMOTION")
                        .build();

                notificationRepository.save(notif);

                // 2. Gửi Email tiếp thị bất đồng bộ (không làm treo HTTP Request)
                String email = recipient.getEmail();
                if (email != null && !email.isBlank() && !email.endsWith("@walkin.local") && !email.endsWith("@guest.local")) {
                    String title = request.getMessageTitle();
                    String content = request.getMessageContent();
                    String salonNameStr = salon.getName();
                    String voucherCodeStr = createdVoucher != null ? createdVoucher.getCode() : null;
                    String endDateStr = formattedEndDate;

                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            String customerName = recipient.getFullName() != null ? recipient.getFullName() : "Quý khách";
                            String voucherHtml = voucherCodeStr != null ? """
                                    <div style="background-color:#fffbe6;border:2px dashed #ffe58f;padding:18px;margin:18px 0;border-radius:10px;text-align:center;">
                                      <p style="margin:0;color:#fa8c16;font-weight:bold;font-size:14px;">MÃ VOUCHER ĐẶC QUYỀN DÀNH CHO BẠN:</p>
                                      <div style="font-size:28px;font-weight:800;color:#d46b08;letter-spacing:3px;margin:10px 0;">%s</div>
                                      <div style="background-color:#fff0f6;color:#c41d7f;display:inline-block;padding:6px 14px;border-radius:20px;font-size:13px;font-weight:bold;">
                                        ⏰ Hạn sử dụng: Đến hết ngày %s
                                      </div>
                                    </div>
                                    """.formatted(voucherCodeStr, !endDateStr.isBlank() ? endDateStr : "14 ngày sau") : "";

                            String htmlBody = """
                                    <div style="font-family:Arial,sans-serif;padding:24px;color:#2c221d;background-color:#f9f6f0;border-radius:12px;">
                                      <h2 style="color:#1890ff;margin-top:0;">%s</h2>
                                      <p>Xin chào <b>%s</b>,</p>
                                      <div style="background-color:#ffffff;padding:16px;border-left:4px solid #1890ff;margin:16px 0;border-radius:8px;">
                                        <p style="margin:0;font-size:15px;line-height:1.6;">%s</p>
                                      </div>
                                      %s
                                      <div style="margin-top:24px;">
                                        <a href="http://localhost:5173" style="background-color:#1890ff;color:#ffffff;padding:12px 24px;text-decoration:none;border-radius:8px;font-weight:bold;display:inline-block;">Đặt Lịch Nhận Ưu Đãi Ngay</a>
                                      </div>
                                      <p style="color:#8c8c8c;font-size:12px;margin-top:24px;">Cảm ơn bạn đã lựa chọn dịch vụ tại %s!</p>
                                    </div>
                                    """.formatted(title, customerName, content, voucherHtml, salonNameStr);

                            emailService.sendNotificationEmail(email, title, htmlBody);
                        } catch (Exception ex) {
                            log.warn("Không thể gửi email chiến dịch tới {}: {}", email, ex.getMessage());
                        }
                    });
                }
            }
        }

        // 4. Lưu vết TargetedCampaign
        TargetedCampaign campaign = TargetedCampaign.builder()
                .salon(salon)
                .branch(branch)
                .campaignName(request.getCampaignName())
                .segmentType(request.getSegmentType().toUpperCase())
                .messageTitle(request.getMessageTitle())
                .messageContent(request.getMessageContent())
                .voucher(createdVoucher)
                .recipientCount(targetCustomers.size())
                .status("COMPLETED")
                .build();

        return targetedCampaignRepository.save(campaign);
    }

    @Override
    public List<TargetedCampaign> getCampaignHistory(Long branchId) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Salon của tài khoản này"));

        if (branchId != null) {
            return targetedCampaignRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        }
        return targetedCampaignRepository.findBySalonIdOrderByCreatedAtDesc(salon.getId());
    }

    // Helper tính toán các chỉ số khách hàng
    private List<CustomerSegmentDetailDto> buildCustomerMetricsList(Long salonId, Long branchId) {
        List<Booking> bookings = branchId != null
                ? bookingRepository.findByBranchId(branchId)
                : bookingRepository.findByBranchSalonId(salonId);

        if (bookings == null || bookings.isEmpty()) {
            return List.of();
        }

        Map<User, List<Booking>> bookingsByCustomer = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getCustomer));

        LocalDate today = LocalDate.now();
        List<CustomerSegmentDetailDto> result = new ArrayList<>();

        for (Map.Entry<User, List<Booking>> entry : bookingsByCustomer.entrySet()) {
            User customer = entry.getKey();
            List<Booking> customerBookings = entry.getValue();

            List<Booking> completedBookings = customerBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .sorted(Comparator.comparing(Booking::getBookingDate))
                    .toList();

            long completedCount = completedBookings.size();
            if (completedCount == 0) {
                continue; // Chưa từng hoàn thành booking nào
            }

            BigDecimal totalSpent = completedBookings.stream()
                    .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            LocalDate firstDate = completedBookings.get(0).getBookingDate();
            LocalDate lastDate = completedBookings.get(completedBookings.size() - 1).getBookingDate();

            long daysSinceLast = ChronoUnit.DAYS.between(lastDate, today);
            long activeMonths = Math.max(1, ChronoUnit.MONTHS.between(firstDate.withDayOfMonth(1), today.withDayOfMonth(1)) + 1);

            // Classification Rules
            String segmentType;
            if (daysSinceLast > 60) {
                segmentType = "AT_RISK";
            } else if (completedCount > 5 || totalSpent.compareTo(BigDecimal.valueOf(5000000)) >= 0) {
                segmentType = "VIP";
            } else if (completedCount >= 2) {
                segmentType = "RETURNING";
            } else {
                segmentType = "NEW";
            }

            // Metrics
            BigDecimal aov = totalSpent.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP);
            double freq = Math.round(((double) completedCount / activeMonths) * 10.0) / 10.0;
            BigDecimal clv = aov.multiply(BigDecimal.valueOf(freq)).multiply(BigDecimal.valueOf(12))
                    .setScale(2, RoundingMode.HALF_UP);

            result.add(CustomerSegmentDetailDto.builder()
                    .customerId(customer.getId())
                    .fullName(customer.getFullName() != null ? customer.getFullName() : customer.getPhone())
                    .phone(customer.getPhone())
                    .avatarUrl(customer.getAvatarUrl())
                    .segmentType(segmentType)
                    .completedBookingsCount(completedCount)
                    .totalSpent(totalSpent)
                    .firstBookingDate(firstDate)
                    .lastBookingDate(lastDate)
                    .daysSinceLastBooking(daysSinceLast)
                    .averageOrderValue(aov)
                    .frequencyPerMonth(freq)
                    .customerLifetimeValue(clv)
                    .build());
        }

        // Sort descending by totalSpent
        result.sort((a, b) -> b.getTotalSpent().compareTo(a.getTotalSpent()));

        return result;
    }
}
