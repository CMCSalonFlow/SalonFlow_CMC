package com.example.salonflow.services.impl;

import com.example.salonflow.entity.Payment;
import com.example.salonflow.services.service.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service("momoService")
public class MoMoServiceImpl implements PaymentGatewayService {

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.url}")
    private String momoUrl;

    @Value("${momo.webhook-url}")
    private String webhookUrl;

    private final WebClient webClient = WebClient.create();

    @Override
    public String createPaymentUrl(Payment payment, String returnUrl) {
        String requestId = payment.getIdempotencyKey();
        String orderId = payment.getIdempotencyKey();
        long amount = payment.getAmount().longValue();
        String orderInfo = "Thanh toan don hang #" + payment.getBooking().getId();
        String requestType = "captureWallet";
        String extraData = "";

        // Ghép chuỗi signature theo chuẩn MoMo
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + webhookUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + returnUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = hmacSHA256(secretKey, rawSignature);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "SalonFlow");
        requestBody.put("storeId", "SalonFlowStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", returnUrl);
        requestBody.put("ipnUrl", webhookUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(momoUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("payUrl")) {
                return (String) response.get("payUrl");
            } else {
                String message = response != null ? (String) response.get("message") : "No response";
                throw new RuntimeException("Loi goi API MoMo: " + message);
            }
        } catch (Exception e) {
            throw new RuntimeException("Loi ket noi MoMo API", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> params, String receivedSignature) {
        String amount = params.get("amount");
        String extraData = params.get("extraData");
        String message = params.get("message");
        String orderId = params.get("orderId");
        String orderInfo = params.get("orderInfo");
        String partnerCode = params.get("partnerCode");
        String requestId = params.get("requestId");
        String responseTime = params.get("responseTime");
        String resultCode = params.get("resultCode");
        String transId = params.get("transId");
        String payType = params.get("payType");

        // Ghép chuỗi signature Webhook MoMo
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + (extraData != null ? extraData : "") +
                "&message=" + message +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&requestId=" + requestId +
                "&responseTime=" + responseTime +
                "&resultCode=" + resultCode +
                "&transId=" + transId +
                "&payType=" + payType;

        String calculatedSignature = hmacSHA256(secretKey, rawSignature);
        return calculatedSignature.equalsIgnoreCase(receivedSignature);
    }

    private String hmacSHA256(String key, String data) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            byte[] result = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Loi ma hoa HMAC SHA256", e);
        }
    }
}
