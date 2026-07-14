package com.example.salonflow.services.service;

import com.example.salonflow.entity.Payment;
import java.util.Map;

/**
 * Interface định nghĩa các phương thức chung cho các cổng thanh toán.
 */
public interface PaymentGatewayService {

    /**
     * Tạo URL thanh toán để Frontend redirect người dùng.
     *
     * @param payment   thực thể thanh toán chứa số tiền, idempotency key
     * @param returnUrl URL redirect ở Frontend sau khi thanh toán xong
     * @return URL thanh toán trực tuyến
     */
    String createPaymentUrl(Payment payment, String returnUrl);

    /**
     * Xác minh chữ ký bảo mật được gửi kèm từ webhook/IPN của cổng thanh toán.
     *
     * @param params            danh sách các tham số nhận được từ webhook
     * @param receivedSignature chữ ký số nhận được từ webhook
     * @return true nếu chữ ký hợp lệ, ngược lại false
     */
    boolean verifyWebhookSignature(Map<String, String> params, String receivedSignature);
}
