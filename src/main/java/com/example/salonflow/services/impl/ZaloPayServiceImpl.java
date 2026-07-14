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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Service("zalopayService")
public class ZaloPayServiceImpl implements PaymentGatewayService {

    @Value("${zalopay.app-id}")
    private String appId;

    @Value("${zalopay.key-1}")
    private String key1;

    @Value("${zalopay.key-2}")
    private String key2;

    @Value("${zalopay.url}")
    private String zalopayUrl;

    private final WebClient webClient = WebClient.create();

    @Override
    public String createPaymentUrl(Payment payment, String returnUrl) {
        String appUser = "SalonFlowUser";
        long amount = payment.getAmount().longValue();
        long appTime = System.currentTimeMillis();
        String item = "[]";
        
        // ZaloPay yêu cầu app_trans_id định dạng yyMMdd_xxxxx
        String todayStr = new SimpleDateFormat("yyMMdd").format(new Date());
        // Sử dụng payment.getId() làm hậu tố đảm bảo tính duy nhất và dễ phân tích khi nhận webhook
        String appTransId = todayStr + "_" + payment.getId();
        
        // Cấu hình redirect url vào embed_data
        String embedData = "{\"redirecturl\":\"" + returnUrl + "\"}";

        // Ghép chuỗi để tính mac theo ZaloPay: app_id|app_trans_id|app_user|amount|app_time|embed_data|item
        String rawMac = appId + "|" + appTransId + "|" + appUser + "|" + amount + "|" + appTime + "|" + embedData + "|" + item;
        String mac = hmacSHA256(key1, rawMac);

        Map<String, Object> requestBody = Map.of(
                "app_id", Integer.parseInt(appId),
                "app_trans_id", appTransId,
                "app_user", appUser,
                "app_time", appTime,
                "amount", amount,
                "item", item,
                "embed_data", embedData,
                "description", "Thanh toan don hang #" + payment.getBooking().getId(),
                "bank_code", "",
                "mac", mac
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(zalopayUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && Integer.valueOf(1).equals(response.get("return_code"))) {
                return (String) response.get("order_url");
            } else {
                String message = response != null ? (String) response.get("return_message") : "No response";
                throw new RuntimeException("Loi goi API ZaloPay: " + message);
            }
        } catch (Exception e) {
            throw new RuntimeException("Loi ket noi ZaloPay API", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> params, String receivedSignature) {
        // ZaloPay verify webhook signature: hmac256(key2, data)
        String data = params.get("data");
        String calculatedSignature = hmacSHA256(key2, data);
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
