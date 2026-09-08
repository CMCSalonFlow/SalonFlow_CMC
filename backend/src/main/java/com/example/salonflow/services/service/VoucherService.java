package com.example.salonflow.services.service;

import com.example.salonflow.dto.voucher.*;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    /** Lấy tất cả voucher (Admin) */
    List<VoucherResponse> getAllVouchers();

    /** Lấy voucher theo salonId (Owner/Manager) */
    List<VoucherResponse> getVouchersBySalonId(Long salonId);

    List<VoucherResponse> getVouchers(Long salonId);

    VoucherResponse createVoucher(CreateVoucherRequest request);

    List<VoucherResponse> createBatchVouchers(CreateVoucherBatchRequest request);

    void deactivateVoucher(Long id);

    ValidateVoucherResponse validateVoucher(String code, BigDecimal orderTotal);

    ValidateVoucherResponse validateVoucher(String code, BigDecimal orderTotal, Long salonId);
}
