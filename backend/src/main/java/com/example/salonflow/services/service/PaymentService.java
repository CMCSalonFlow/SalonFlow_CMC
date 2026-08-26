package com.example.salonflow.services.service;

import com.example.salonflow.dto.payment.CreatePaymentUrlRequest;
import com.example.salonflow.dto.payment.PaymentResponse;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {

    /**
     * Tạo URL thanh toán và lưu thông tin Payment (Idempotency Key).
     */
    PaymentResponse createPaymentUrl(CreatePaymentUrlRequest request);

    /**
     * Kiểm tra trạng thái thanh toán mới nhất của một Booking.
     */
    PaymentResponse getPaymentStatus(Long bookingId);


    /**
     * Xử lý thanh toán tiền mặt tại quầy (POS Mode) bỏ qua các cổng trực tuyến.
     */
    PaymentResponse processPosCashPayment(com.example.salonflow.dto.payment.PosCashPaymentRequest request);

    /**
     * Tự động xác nhận thanh toán chuyển khoản ngân hàng (VietQR / Bank Transfer).
     */
    PaymentResponse autoConfirmBankTransfer(Long bookingId);

    /**
     * Tiếp nhận và xử lý Webhook tự động từ SePay / Casso / Ngân hàng.
     */
    PaymentResponse processSepayWebhook(Map<String, Object> payload);
}
