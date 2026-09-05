package com.example.salonflow.services.impl;

import com.example.salonflow.dto.loyalty.LoyaltySummaryResponse;
import com.example.salonflow.dto.loyalty.LoyaltyTransactionResponse;
import com.example.salonflow.dto.loyalty.RedeemPointsRequest;
import com.example.salonflow.dto.loyalty.RedeemPointsResponse;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.CustomerProfile;
import com.example.salonflow.entity.LoyaltyPoint;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.Voucher;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.DiscountType;
import com.example.salonflow.entity.enums.LoyaltyTransactionType;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.CustomerProfileRepository;
import com.example.salonflow.repository.LoyaltyPointRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.repository.VoucherRepository;
import com.example.salonflow.services.service.LoyaltyPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyPointServiceImpl implements LoyaltyPointService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void earnPointsForBooking(Long userId, BigDecimal orderTotal, String bookingReferenceId) {
        if (userId == null || orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        int pointsEarned = orderTotal.divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR).intValue();
        if (pointsEarned <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusYears(1);

        LoyaltyPoint transaction = LoyaltyPoint.builder()
                .userId(userId)
                .transactionType(LoyaltyTransactionType.EARN)
                .points(pointsEarned)
                .referenceId(bookingReferenceId)
                .expiresAt(expiresAt)
                .build();

        loyaltyPointRepository.save(transaction);
        updateUserProfilePoints(userId);
        log.info("Earned {} loyalty points for user {} from booking {}", pointsEarned, userId, bookingReferenceId);
    }

    @Transactional
    public void syncLoyaltyPointsFromCompletedBookings(Long userId) {
        if (userId == null) return;
        try {
            User user = userRepository.findById(userId).orElse(null);
            String userEmail = user != null && user.getEmail() != null ? user.getEmail().trim() : "";
            String userPhone = user != null && user.getPhone() != null ? user.getPhone().trim() : "";

            List<Booking> completedBookings = bookingRepository.findBookingsForUserOrContact(userId, userEmail, userPhone);

            boolean updated = false;
            if (completedBookings != null && !completedBookings.isEmpty()) {
                for (Booking booking : completedBookings) {
                    if (booking.getStatus() == BookingStatus.COMPLETED && booking.getTotalPrice() != null && booking.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                        String refId = "BOOKING:" + booking.getId();
                        if (!loyaltyPointRepository.existsByUserIdAndReferenceId(userId, refId)) {
                            int pointsEarned = booking.getTotalPrice().divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR).intValue();
                            if (pointsEarned > 0) {
                                LocalDateTime createdDate = booking.getCreatedAt() != null
                                        ? LocalDateTime.ofInstant(booking.getCreatedAt(), java.time.ZoneId.systemDefault())
                                        : LocalDateTime.now();
                                LoyaltyPoint transaction = LoyaltyPoint.builder()
                                        .userId(userId)
                                        .transactionType(LoyaltyTransactionType.EARN)
                                        .points(pointsEarned)
                                        .referenceId(refId)
                                        .expiresAt(createdDate.plusYears(1))
                                        .createdAt(createdDate)
                                        .build();
                                loyaltyPointRepository.save(transaction);
                                updated = true;
                                log.info("Auto-synced {} loyalty points for user {} from past COMPLETED booking #{}", pointsEarned, userId, booking.getId());
                            }
                        }
                    }
                }
            }

            // Safety Net: If CustomerProfile has loyaltyPoints > current SUM in loyalty_points table
            CustomerProfile profile = customerProfileRepository.findByUser_Id(userId).orElse(null);
            if (profile != null && profile.getLoyaltyPoints() != null && profile.getLoyaltyPoints() > 0) {
                Integer dbPoints = loyaltyPointRepository.findTotalPointsByUserId(userId, LocalDateTime.now());
                int currentSum = dbPoints != null ? dbPoints : 0;
                if (profile.getLoyaltyPoints() > currentSum) {
                    int diff = profile.getLoyaltyPoints() - currentSum;
                    if (!loyaltyPointRepository.existsByUserIdAndReferenceId(userId, "INITIAL_BALANCE")) {
                        LoyaltyPoint initTx = LoyaltyPoint.builder()
                                .userId(userId)
                                .transactionType(LoyaltyTransactionType.EARN)
                                .points(diff)
                                .referenceId("INITIAL_BALANCE")
                                .expiresAt(LocalDateTime.now().plusYears(1))
                                .createdAt(LocalDateTime.now())
                                .build();
                        loyaltyPointRepository.save(initTx);
                        updated = true;
                    }
                }
            }

            if (updated) {
                updateUserProfilePoints(userId);
            }
        } catch (Exception e) {
            log.error("Failed to sync loyalty points for user {}: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public LoyaltySummaryResponse getUserLoyaltySummary(Long userId) {
        if (userId == null) {
            return LoyaltySummaryResponse.builder()
                    .totalPoints(0)
                    .activePoints(0)
                    .equivalentVoucherValue(0)
                    .expiringPoints(0)
                    .memberRank("BRONZE")
                    .build();
        }

        try {
            syncLoyaltyPointsFromCompletedBookings(userId);
        } catch (Exception e) {
            log.error("Sync error in getUserLoyaltySummary for user {}: {}", userId, e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        Integer totalPoints = loyaltyPointRepository.findTotalPointsByUserId(userId, now);
        if (totalPoints == null) totalPoints = 0;

        CustomerProfile profile = customerProfileRepository.findByUser_Id(userId).orElse(null);
        if (profile != null && profile.getLoyaltyPoints() != null && profile.getLoyaltyPoints() > totalPoints) {
            totalPoints = profile.getLoyaltyPoints();
        }

        Integer expiringPoints = loyaltyPointRepository.findExpiringPointsByUserId(userId, now, now.plusDays(30));
        if (expiringPoints == null) expiringPoints = 0;

        String rank = "BRONZE";
        if (totalPoints >= 10000) rank = "DIAMOND";
        else if (totalPoints >= 5000) rank = "GOLD";
        else if (totalPoints >= 2000) rank = "SILVER";

        int equivalentVoucherValue = (totalPoints / 100) * 10000;

        return LoyaltySummaryResponse.builder()
                .totalPoints(totalPoints)
                .activePoints(totalPoints)
                .equivalentVoucherValue(equivalentVoucherValue)
                .expiringPoints(expiringPoints)
                .memberRank(rank)
                .build();
    }

    @Override
    @Transactional
    public List<LoyaltyTransactionResponse> getUserTransactionHistory(Long userId) {
        if (userId == null) {
            return List.of();
        }

        try {
            syncLoyaltyPointsFromCompletedBookings(userId);
        } catch (Exception e) {
            log.error("Sync error in getUserTransactionHistory for user {}: {}", userId, e.getMessage());
        }

        return loyaltyPointRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RedeemPointsResponse redeemPointsForVoucher(Long userId, RedeemPointsRequest request) {
        syncLoyaltyPointsFromCompletedBookings(userId);
        LocalDateTime now = LocalDateTime.now();
        Integer currentPoints = loyaltyPointRepository.findTotalPointsByUserId(userId, now);

        int pointsToRedeem = request.getPointsToRedeem();
        if (pointsToRedeem < 100 || pointsToRedeem % 100 != 0) {
            throw new IllegalArgumentException("Points to redeem must be a multiple of 100 (100 points = 10,000 VND)");
        }

        if (currentPoints < pointsToRedeem) {
            throw new IllegalArgumentException("Insufficient loyalty points balance");
        }

        BigDecimal voucherAmount = BigDecimal.valueOf((long) pointsToRedeem / 100 * 10000);

        String voucherCode = "LOYALTY-" + pointsToRedeem + "PTS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Voucher voucher = Voucher.builder()
                .code(voucherCode)
                .discountType(DiscountType.FIXED)
                .discountValue(voucherAmount)
                .minOrderAmount(BigDecimal.ZERO)
                .usageLimit(1)
                .usedCount(0)
                .startDate(now)
                .endDate(now.plusDays(90))
                .isActive(true)
                .userId(userId)
                .build();

        voucherRepository.save(voucher);

        LoyaltyPoint transaction = LoyaltyPoint.builder()
                .userId(userId)
                .transactionType(LoyaltyTransactionType.REDEEM)
                .points(-pointsToRedeem)
                .referenceId("VOUCHER:" + voucherCode)
                .expiresAt(null)
                .build();

        loyaltyPointRepository.save(transaction);
        updateUserProfilePoints(userId);

        Integer remainingPoints = currentPoints - pointsToRedeem;

        return RedeemPointsResponse.builder()
                .voucherCode(voucherCode)
                .discountValue(voucherAmount)
                .pointsRedeemed(pointsToRedeem)
                .remainingPoints(remainingPoints)
                .build();
    }

    @Override
    @Transactional
    public void expirePointsJob() {
        LocalDateTime now = LocalDateTime.now();
        List<LoyaltyPoint> expiredList = loyaltyPointRepository.findExpiredPoints(now);
        for (LoyaltyPoint lp : expiredList) {
            LoyaltyPoint expireTx = LoyaltyPoint.builder()
                    .userId(lp.getUserId())
                    .transactionType(LoyaltyTransactionType.EXPIRED)
                    .points(-lp.getPoints())
                    .referenceId("EXPIRED:" + lp.getId())
                    .expiresAt(null)
                    .build();
            loyaltyPointRepository.save(expireTx);

            lp.setPoints(0);
            loyaltyPointRepository.save(lp);
            updateUserProfilePoints(lp.getUserId());
        }
    }

    private void updateUserProfilePoints(Long userId) {
        CustomerProfile profile = customerProfileRepository.findByUser_Id(userId).orElse(null);
        if (profile != null) {
            Integer totalPoints = loyaltyPointRepository.findTotalPointsByUserId(userId, LocalDateTime.now());
            profile.setLoyaltyPoints(totalPoints);
            customerProfileRepository.save(profile);
        }
    }

    private LoyaltyTransactionResponse toTransactionResponse(LoyaltyPoint lp) {
        return LoyaltyTransactionResponse.builder()
                .id(lp.getId())
                .transactionType(lp.getTransactionType())
                .points(lp.getPoints())
                .referenceId(lp.getReferenceId())
                .expiresAt(lp.getExpiresAt())
                .createdAt(lp.getCreatedAt())
                .build();
    }
}
