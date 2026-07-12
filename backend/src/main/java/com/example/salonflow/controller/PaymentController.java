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
     * Webhook/IPN nhận kết quả thanh toán từ VNPay.
     */
    @GetMapping("/vnpay/webhook")
    public ResponseEntity<Map<String, String>> vnpayWebhook(
            @RequestParam Map<String, String> params
    ) {
        try {
            paymentService.processVNPayWebhook(params);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm success"));
        } catch (IllegalArgumentException e) {
            // Sai chữ ký số
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Signature: " + e.getMessage()));
        } catch (IllegalStateException e) {
            // Lỗi nghiệp vụ (ví dụ: giao dịch đã xử lý rồi)
            return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error: " + e.getMessage()));
        }
    }

    /**
     * Webhook nhận kết quả thanh toán từ MoMo.
     */
    @PostMapping("/momo/webhook")
    public ResponseEntity<Void> momoWebhook(
            @RequestBody Map<String, String> body
    ) {
        try {
            paymentService.processMoMoWebhook(body);
            return ResponseEntity.noContent().build(); // HTTP 204
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Webhook nhận kết quả thanh toán từ ZaloPay.
     */
    @PostMapping("/zalopay/webhook")
    public ResponseEntity<Map<String, Object>> zalopayWebhook(
            @RequestBody Map<String, String> body
    ) {
        Map<String, Object> result = paymentService.processZaloPayWebhook(body);
        return ResponseEntity.ok(result);
    }
}
