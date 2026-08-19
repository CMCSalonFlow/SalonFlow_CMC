package com.example.salonflow.services.impl;

import com.example.salonflow.dto.voucher.*;
import com.example.salonflow.entity.Voucher;
import com.example.salonflow.entity.enums.DiscountType;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.VoucherRepository;
import com.example.salonflow.services.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public List<VoucherResponse> getAllVouchers() {
        return voucherRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (voucherRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Mã voucher '" + code + "' đã tồn tại!");
        }

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
                .build();
    }
}
