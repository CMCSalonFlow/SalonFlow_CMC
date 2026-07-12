package com.example.salonflow.services.service;

import com.example.salonflow.dto.payment.CreatePaymentUrlRequest;
import com.example.salonflow.dto.payment.PaymentResponse;

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
     * Xử lý IPN Webhook từ VNPay.
     */
    void processVNPayWebhook(Map<String, String> params);

    /**
     * Xử lý Webhook từ MoMo.
     */
    void processMoMoWebhook(Map<String, String> params);

    /**
     * Xử lý Webhook từ ZaloPay.
     * ZaloPay Webhook gửi data (JSON) và mac (chữ ký).
     */
    Map<String, Object> processZaloPayWebhook(Map<String, String> params);
}
