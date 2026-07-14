package com.example.salonflow.services.service;

import com.example.salonflow.dto.voucher.*;
import com.example.salonflow.entity.Booking;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {

    /** Tạo 1 voucher đơn */
    VoucherResponse createVoucher(CreateVoucherRequest request);

    /** Tạo batch voucher (sinh code tự động theo prefix) */
    List<VoucherResponse> createBatch(BatchCreateVoucherRequest request);

    /** Validate voucher cho user hiện tại, optionally tính discountAmount từ orderTotal */
    ValidateVoucherResponse validate(String code, BigDecimal orderTotal);

    /** Đánh dấu voucher đã dùng bởi user + gắn với booking */
    void redeemVoucher(String code, Long userId, Booking booking);

    /** Lấy tất cả voucher (admin) */
    List<VoucherResponse> getAll();

    /** Deactivate voucher */
    VoucherResponse deactivate(Long id);
}
