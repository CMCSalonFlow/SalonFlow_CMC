package com.example.salonflow.services.impl;

import com.example.salonflow.entity.Payment;
import com.example.salonflow.services.service.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("vnpayService")
public class VNPayServiceImpl implements PaymentGatewayService {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @Override
    public String createPaymentUrl(Payment payment, String returnUrl) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        
        // VNPay yêu cầu số tiền nhân 100
        long amount = payment.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        
        // Dùng idempotency key làm vnp_TxnRef để đảm bảo tính duy nhất
        vnpParams.put("vnp_TxnRef", payment.getIdempotencyKey());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang #" + payment.getBooking().getId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Sắp xếp các key theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnpSecureHash = hmacSHA512(hashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        return vnpayUrl + "?" + queryUrl;
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> params, String receivedSignature) {
        // Loại bỏ signature ra khỏi danh sách tham số để tính hash
        Map<String, String> hashParams = new HashMap<>(params);
        hashParams.remove("vnp_SecureHash");
        hashParams.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(hashParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = hashParams.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        String calculatedSignature = hmacSHA512(hashSecret, hashData.toString());
        return calculatedSignature.equalsIgnoreCase(receivedSignature);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Loi ma hoa HMAC SHA512", e);
        }
    }
}
