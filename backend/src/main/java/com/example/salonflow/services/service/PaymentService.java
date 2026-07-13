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
     * Xác thực thông tin thanh toán từ callback của VNPay.
     */
    PaymentResponse verifyPayment(Map<String, String> params);

    /**
     * Xác thực thông tin thanh toán từ IPN của VNPay.
     */
    Map<String, String> verifyIpn(Map<String, String> params);
}
