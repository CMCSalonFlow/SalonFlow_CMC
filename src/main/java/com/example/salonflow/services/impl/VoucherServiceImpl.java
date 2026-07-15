package com.example.salonflow.services.impl;

import com.example.salonflow.dto.voucher.*;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.Voucher;
import com.example.salonflow.entity.VoucherUsage;
import com.example.salonflow.entity.enums.DiscountType;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.repository.VoucherRepository;
import com.example.salonflow.repository.VoucherUsageRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // tránh nhầm lẫn O/0, I/1
    private static final int SUFFIX_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final UserRepository userRepository;

    // ─── Create single ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        validateUniqueCode(request.getCode());
        validateDiscountValue(request.getDiscountType(), request.getDiscountValue());

        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxUses(request.getMaxUses())
                .expiresAt(request.getExpiresAt())
                .build();

        return VoucherResponse.from(voucherRepository.save(voucher));
    }

    // ─── Create batch ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<VoucherResponse> createBatch(BatchCreateVoucherRequest request) {
        validateDiscountValue(request.getDiscountType(), request.getDiscountValue());

        List<Voucher> vouchers = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = request.getQuantity() * 10;

        while (vouchers.size() < request.getQuantity() && attempts < maxAttempts) {
            attempts++;
            String code = request.getPrefix() + "_" + generateSuffix();
            if (!voucherRepository.existsByCode(code)) {
                vouchers.add(Voucher.builder()
                        .code(code)
                        .discountType(request.getDiscountType())
                        .discountValue(request.getDiscountValue())
                        .maxUses(1) // AC: 1 lần/user, batch luôn maxUses=1
                        .expiresAt(request.getExpiresAt())
                        .build());
            }
        }

        if (vouchers.size() < request.getQuantity()) {
            throw new BadRequestException("Không thể sinh đủ code unique. Thử prefix khác.");
        }

        return voucherRepository.saveAll(vouchers)
                .stream()
                .map(VoucherResponse::from)
                .collect(Collectors.toList());
    }

    // ─── Validate ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ValidateVoucherResponse validate(String code, BigDecimal orderTotal) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Voucher voucher = voucherRepository.findByCode(code.toUpperCase())
                .orElse(null);

        if (voucher == null) {
            return invalidResponse("Voucher không tồn tại.");
        }
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            return invalidResponse("Voucher đã bị vô hiệu hóa.");
        }
        if (LocalDateTime.now().isAfter(voucher.getExpiresAt())) {
            return invalidResponse("Voucher đã hết hạn.");
        }
        if (voucher.getUsedCount() >= voucher.getMaxUses()) {
            return invalidResponse("Voucher đã hết lượt sử dụng.");
        }
        if (voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), currentUserId)) {
            return invalidResponse("Bạn đã sử dụng voucher này rồi.");
        }

        BigDecimal discountAmount = calculateDiscount(voucher, orderTotal);

        return ValidateVoucherResponse.builder()
                .valid(true)
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .discountAmount(discountAmount)
                .message("Voucher hợp lệ.")
                .build();
    }

    // ─── Redeem ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void redeemVoucher(String code, Long userId, Booking booking) {
        Voucher voucher = voucherRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher không tồn tại: " + code));

        if (!voucher.isValid()) {
            throw new BadRequestException("Voucher không còn hợp lệ.");
        }
        if (voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), userId)) {
            throw new BadRequestException("User đã sử dụng voucher này rồi.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + userId));

        VoucherUsage usage = VoucherUsage.builder()
                .voucher(voucher)
                .user(user)
                .booking(booking)
                .build();
        voucherUsageRepository.save(usage);

        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
    }

    // ─── Admin ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAll() {
        return voucherRepository.findAll()
                .stream()
                .map(VoucherResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VoucherResponse deactivate(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher không tồn tại: " + id));
        voucher.setIsActive(false);
        return VoucherResponse.from(voucherRepository.save(voucher));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void validateUniqueCode(String code) {
        if (voucherRepository.existsByCode(code.toUpperCase())) {
            throw new BadRequestException("Code đã tồn tại: " + code);
        }
    }

    private void validateDiscountValue(DiscountType type, BigDecimal value) {
        if (type == DiscountType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Giảm theo phần trăm không được vượt quá 100%.");
        }
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (voucher.getDiscountType() == DiscountType.FIXED) {
            // Không giảm vượt quá tổng đơn hàng
            return voucher.getDiscountValue().min(orderTotal);
        } else {
            return orderTotal
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
    }

    private ValidateVoucherResponse invalidResponse(String message) {
        return ValidateVoucherResponse.builder()
                .valid(false)
                .message(message)
                .build();
    }

    private String generateSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
