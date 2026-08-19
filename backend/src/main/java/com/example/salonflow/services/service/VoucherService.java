package com.example.salonflow.services.service;

import com.example.salonflow.dto.voucher.*;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    List<VoucherResponse> getAllVouchers();

    VoucherResponse createVoucher(CreateVoucherRequest request);

    List<VoucherResponse> createBatchVouchers(CreateVoucherBatchRequest request);

    void deactivateVoucher(Long id);

    ValidateVoucherResponse validateVoucher(String code, BigDecimal orderTotal);
}
