package com.example.salonflow.services.impl;

import com.example.salonflow.dto.voucher.*;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.Voucher;
import com.example.salonflow.entity.enums.DiscountType;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.repository.VoucherRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final SalonRepository salonRepository;

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    @Override
    public List<VoucherResponse> getAllVouchers() {
        if (isAdmin()) {
            return voucherRepository.findAll()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        // Tự động kiểm tra nếu user đăng nhập là Owner của Salon thì chỉ trả về voucher của salon đó
        Optional<Long> currentUserIdOpt = SecurityUtils.getCurrentUserIdOptional();
        if (currentUserIdOpt.isPresent()) {
            Optional<Salon> salonOpt = salonRepository.findFirstByOwnerId(currentUserIdOpt.get());
            if (salonOpt.isPresent()) {
                return getVouchersBySalonId(salonOpt.get().getId());
            }
            // User có tài khoản / role salon owner nhưng CHƯA TẠO SALON -> danh sách rỗng, tuyệt đối không trả về tất cả
            return List.of();
        }

        return List.of();
    }

    @Override
    public List<VoucherResponse> getVouchersBySalonId(Long salonId) {
        if (salonId == null) {
            return getAllVouchers();
        }
        return voucherRepository.findBySalonId(salonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<VoucherResponse> getVouchers(Long salonId) {
        if (isAdmin()) {
            if (salonId != null) {
                return getVouchersBySalonId(salonId);
            }
            return voucherRepository.findAll().stream().map(this::toResponse).toList();
        }

        Optional<Long> currentUserIdOpt = SecurityUtils.getCurrentUserIdOptional();
        if (currentUserIdOpt.isPresent()) {
            Optional<Salon> salonOpt = salonRepository.findFirstByOwnerId(currentUserIdOpt.get());
            if (salonOpt.isPresent()) {
                Long ownedSalonId = salonOpt.get().getId();
                if (salonId != null && !salonId.equals(ownedSalonId)) {
                    return List.of(); // Không cho phép xem voucher của salon khác
                }
                return getVouchersBySalonId(ownedSalonId);
            }
            // Chưa tạo salon
            return List.of();
        }

        if (salonId != null) {
            return getVouchersBySalonId(salonId);
        }

        return List.of();
    }

    private Long resolveSalonId(Long requestedSalonId) {
        if (isAdmin() && requestedSalonId != null) {
            return requestedSalonId;
        }
        Optional<Long> currentUserIdOpt = SecurityUtils.getCurrentUserIdOptional();
        if (currentUserIdOpt.isPresent()) {
            Optional<Salon> salonOpt = salonRepository.findFirstByOwnerId(currentUserIdOpt.get());
            if (salonOpt.isPresent()) {
                return salonOpt.get().getId();
            }
            throw new IllegalArgumentException("Bạn chưa tạo salon! Vui lòng tạo thông tin salon trước khi tạo voucher.");
        }
        return requestedSalonId;
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (voucherRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Mã voucher '" + code + "' đã tồn tại!");
        }

        Long salonId = resolveSalonId(request.getSalonId());

        Voucher voucher = Voucher.builder()
                .code(code)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .usageLimit(request.getMaxUses() != null ? request.getMaxUses() : 1)
                .usedCount(0)
                .startDate(LocalDateTime.now())
                .endDate(request.getExpiresAt())
                .isActive(true)
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .salonId(salonId) // ✅ Gắn salon owner
                .build();

        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public List<VoucherResponse> createBatchVouchers(CreateVoucherBatchRequest request) {
        String prefix = request.getPrefix().trim().toUpperCase();
        int quantity = request.getQuantity();
        List<Voucher> vouchersToSave = new ArrayList<>();
        Random random = new Random();

        Long salonId = resolveSalonId(request.getSalonId());

        for (int i = 0; i < quantity; i++) {
            String randomCode;
            int attempts = 0;
            do {
                int randomNum = 100000 + random.nextInt(900000);
                randomCode = prefix + "_" + randomNum;
                attempts++;
            } while (voucherRepository.findByCode(randomCode).isPresent() && attempts < 50);

            Voucher voucher = Voucher.builder()
                    .code(randomCode)
                    .discountType(request.getDiscountType())
                    .discountValue(request.getDiscountValue())
                    .usageLimit(request.getMaxUses() != null ? request.getMaxUses() : 1)
                    .usedCount(0)
                    .startDate(LocalDateTime.now())
                    .endDate(request.getExpiresAt())
                    .isActive(true)
                    .salonId(salonId) // ✅ Gắn salon owner
                    .build();

            vouchersToSave.add(voucher);
        }

        List<Voucher> saved = voucherRepository.saveAll(vouchersToSave);
        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void deactivateVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher id: " + id));
        voucher.setIsActive(false);
        voucherRepository.save(voucher);
    }

    @Override
    public ValidateVoucherResponse validateVoucher(String code, BigDecimal orderTotal) {
        return validateVoucher(code, orderTotal, null);
    }

    @Override
    public ValidateVoucherResponse validateVoucher(String code, BigDecimal orderTotal, Long salonId) {
        if (code == null || code.isBlank()) {
            return ValidateVoucherResponse.builder()
                    .valid(false)
                    .message("Mã voucher không hợp lệ")
                    .build();
        }

        Voucher voucher = voucherRepository.findByCode(code.trim().toUpperCase())
                .orElse(null);

        if (voucher == null || Boolean.FALSE.equals(voucher.getIsActive())) {
            return ValidateVoucherResponse.builder()
                    .valid(false)
                    .message("Mã voucher không tồn tại hoặc đã bị tắt")
                    .build();
        }

        // Kiểm tra xem voucher có thuộc về đúng salon không
        if (voucher.getSalonId() != null) {
            if (salonId == null || !voucher.getSalonId().equals(salonId)) {
                return ValidateVoucherResponse.builder()
                        .valid(false)
                        .message("Mã voucher này không áp dụng cho salon đã chọn")
                        .build();
            }
        }

        if (voucher.getEndDate() != null && LocalDateTime.now().isAfter(voucher.getEndDate())) {
            return ValidateVoucherResponse.builder()
                    .valid(false)
                    .message("Mã voucher đã hết hạn sử dụng")
                    .build();
        }

        int maxUses = voucher.getUsageLimit() != null ? voucher.getUsageLimit() : 1;
        int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        if (used >= maxUses) {
            return ValidateVoucherResponse.builder()
                    .valid(false)
                    .message("Mã voucher đã hết số lần sử dụng")
                    .build();
        }

        if (voucher.getMinOrderAmount() != null && orderTotal != null) {
            if (orderTotal.compareTo(voucher.getMinOrderAmount()) < 0) {
                return ValidateVoucherResponse.builder()
                        .valid(false)
                        .message("Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getMinOrderAmount() + "đ")
                        .build();
            }
        }

        BigDecimal calculatedDiscount = BigDecimal.ZERO;
        if (orderTotal != null) {
            if (voucher.getDiscountType() == DiscountType.FIXED) {
                calculatedDiscount = voucher.getDiscountValue();
            } else if (voucher.getDiscountType() == DiscountType.PERCENT) {
                calculatedDiscount = orderTotal.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (voucher.getMaxDiscountAmount() != null && calculatedDiscount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                    calculatedDiscount = voucher.getMaxDiscountAmount();
                }
            }
        }

        return ValidateVoucherResponse.builder()
                .valid(true)
                .message("Áp dụng voucher thành công!")
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .calculatedDiscount(calculatedDiscount)
                .build();
    }

    private VoucherResponse toResponse(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .maxUses(v.getUsageLimit() != null ? v.getUsageLimit() : 1)
                .usedCount(v.getUsedCount() != null ? v.getUsedCount() : 0)
                .expiresAt(v.getEndDate())
                .isActive(v.getIsActive())
                .minOrderAmount(v.getMinOrderAmount())
                .maxDiscountAmount(v.getMaxDiscountAmount())
                .salonId(v.getSalonId()) // ✅ Include salonId in response
                .build();
    }
}
