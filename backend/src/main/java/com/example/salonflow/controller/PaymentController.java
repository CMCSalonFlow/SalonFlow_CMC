package com.example.salonflow.controller;

import com.example.salonflow.dto.payment.CreatePaymentUrlRequest;
import com.example.salonflow.dto.payment.PaymentResponse;
import com.example.salonflow.services.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Khởi tạo yêu cầu thanh toán trực tuyến, trả về URL thanh toán.
     */
    @PostMapping("/create-url")
    public ResponseEntity<PaymentResponse> createPaymentUrl(
            @Valid @RequestBody CreatePaymentUrlRequest request
    ) {
        PaymentResponse response = paymentService.createPaymentUrl(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy trạng thái giao dịch thanh toán mới nhất của một lịch hẹn.
     */
    @GetMapping("/status/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable Long bookingId
    ) {
        PaymentResponse response = paymentService.getPaymentStatus(bookingId);
        return ResponseEntity.ok(response);
    }

    /**
     * Tiếp nhận phản hồi kết quả thanh toán từ VNPay (Redirect trên client).
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<PaymentResponse> handleVNPayCallback(
            @RequestParam Map<String, String> params
    ) {
        PaymentResponse response = paymentService.verifyPayment(params);
        return ResponseEntity.ok(response);
    }

    /**
     * Nhận phản hồi IPN trực tiếp từ server VNPay.
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> handleVNPayIpn(
            @RequestParam Map<String, String> params
    ) {
        Map<String, String> response = paymentService.verifyIpn(params);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint riêng xử lý Thanh toán Tiền Mặt tại quầy (POS Mode) do Staff xác nhận.
     * Tạo bản ghi Payment (method=CASH, status=SUCCESS, confirmedBy=staffId), không qua cổng thanh toán.
     */
    @PostMapping("/pos/cash")
    public ResponseEntity<PaymentResponse> processPosCashPayment(
            @Valid @RequestBody com.example.salonflow.dto.payment.PosCashPaymentRequest request
    ) {
        PaymentResponse response = paymentService.processPosCashPayment(request);
        return ResponseEntity.ok(response);
    }
}
