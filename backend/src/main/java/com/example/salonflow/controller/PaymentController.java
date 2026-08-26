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

    /**
     * Endpoint nhận Webhook tự động từ SePay / Casso / Ngân hàng khi tiền về tài khoản.
     */
    @PostMapping("/sepay-webhook")
    public ResponseEntity<PaymentResponse> handleSepayWebhook(
            @RequestBody Map<String, Object> payload
    ) {
        PaymentResponse response = paymentService.processSepayWebhook(payload);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint tự động xác nhận chuyển khoản ngân hàng thành công cho Booking ID.
     */
    @PostMapping("/auto-confirm/{bookingId}")
    public ResponseEntity<PaymentResponse> autoConfirmBankTransfer(
            @PathVariable Long bookingId
    ) {
        PaymentResponse response = paymentService.autoConfirmBankTransfer(bookingId);
        return ResponseEntity.ok(response);
    }
}
