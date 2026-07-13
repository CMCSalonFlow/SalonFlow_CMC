package com.example.salonflow.services.impl;

import com.example.salonflow.dto.payment.CreatePaymentUrlRequest;
import com.example.salonflow.dto.payment.PaymentResponse;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Payment;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.PaymentMethod;
import com.example.salonflow.entity.enums.PaymentStatus;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.PaymentRepository;
import com.example.salonflow.services.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Override
    @Transactional
    public PaymentResponse createPaymentUrl(CreatePaymentUrlRequest request) {
        // Kiểm tra Idempotency Key
        Optional<Payment> existingPaymentOpt = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();
            if (existingPayment.getStatus() == PaymentStatus.SUCCESS) {
                throw new IllegalStateException("Giao dich voi idempotency key nay da thanh toan thanh cong");
            }
            if (existingPayment.getStatus() == PaymentStatus.PENDING && existingPayment.getPaymentUrl() != null) {
                return mapToResponse(existingPayment);
            }
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay lich hen voi ID: " + request.getBookingId()));

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Khong the thanh toan cho lich hen o trang thai: " + booking.getStatus());
        }

        // Tạo bản ghi Payment ở trạng thái PENDING
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(request.getPaymentMethod())
                .amount(booking.getTotalPrice()) // Thanh toán toàn bộ tổng tiền
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        payment = paymentRepository.save(payment);

        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            try {
                HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
                String clientIp = getClientIp(servletRequest);

                String vnp_Version = "2.1.0";
                String vnp_Command = "pay";
                String orderType = "other";
                long amount = booking.getTotalPrice().multiply(new BigDecimal(100)).longValue();
                String vnp_TxnRef = payment.getId().toString();

                Map<String, String> vnp_Params = new HashMap<>();
                vnp_Params.put("vnp_Version", vnp_Version);
                vnp_Params.put("vnp_Command", vnp_Command);
                vnp_Params.put("vnp_TmnCode", tmnCode);
                vnp_Params.put("vnp_Amount", String.valueOf(amount));
                vnp_Params.put("vnp_CurrCode", "VND");
                vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
                vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
                vnp_Params.put("vnp_OrderType", orderType);
                vnp_Params.put("vnp_Locale", "vn");
                vnp_Params.put("vnp_ReturnUrl", request.getReturnUrl());
                vnp_Params.put("vnp_IpAddr", clientIp);

                Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                String vnp_CreateDate = formatter.format(cld.getTime());
                vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

                cld.add(Calendar.MINUTE, 15);
                String vnp_ExpireDate = formatter.format(cld.getTime());
                vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

                List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
                Collections.sort(fieldNames);
                List<String> parts = new ArrayList<>();
                for (String fieldName : fieldNames) {
                    String fieldValue = vnp_Params.get(fieldName);
                    if (fieldValue != null && !fieldValue.isEmpty()) {
                        parts.add(encode(fieldName) + "=" + encode(fieldValue));
                    }
                }
                String hashData = String.join("&", parts);
                log.info("VNPay Config - tmnCode: {}, hashSecret: {}, payUrl: {}", tmnCode, hashSecret, payUrl);
                log.info("VNPay HashData for signing: {}", hashData);
                String vnp_SecureHash = hmacSHA512(hashSecret, hashData);
                log.info("VNPay Computed Hash: {}", vnp_SecureHash);
                String queryUrl = hashData + "&vnp_SecureHash=" + vnp_SecureHash;
                String generatedUrl = payUrl + "?" + queryUrl;
                log.info("VNPay Generated URL: {}", generatedUrl);

                payment.setPaymentUrl(generatedUrl);
                payment = paymentRepository.save(payment);
            } catch (Exception e) {
                log.error("Loi khi tao URL thanh toan VNPay", e);
                throw new IllegalStateException("Loi khi tao URL thanh toan VNPay: " + e.getMessage());
            }
        }

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(Long bookingId) {
        Payment payment = paymentRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay giao dich thanh toan cho Booking ID: " + bookingId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
            throw new IllegalArgumentException("Khong tim thay Secure Hash");
        }

        // 1. Verify Signature
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty() && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                fields.put(key, value);
            }
        }

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        List<String> parts = new ArrayList<>();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            parts.add(encode(fieldName) + "=" + encode(fieldValue));
        }
        String hashData = String.join("&", parts);
        String computedHash = hmacSHA512(hashSecret, hashData);

        if (!computedHash.equalsIgnoreCase(vnp_SecureHash)) {
            log.error("Signature verification failed. Computed: {}, Received: {}", computedHash, vnp_SecureHash);
            throw new IllegalArgumentException("Chu ky khong hop le");
        }

        // 2. Process payment status
        String vnp_TxnRef = params.get("vnp_TxnRef");
        if (vnp_TxnRef == null) {
            throw new IllegalArgumentException("Khong tim thay ma giao dich (vnp_TxnRef)");
        }

        Long paymentId = Long.parseLong(vnp_TxnRef);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Giao dich khong ton tai: " + paymentId));

        // Check amount
        BigDecimal vnpAmount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));
        if (payment.getAmount().compareTo(vnpAmount) != 0) {
            throw new IllegalArgumentException("So tien khong khop");
        }

        // Check order status
        if (payment.getStatus() == PaymentStatus.PENDING) {
            String responseCode = params.get("vnp_ResponseCode");
            Booking booking = payment.getBooking();
            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                booking.setStatus(BookingStatus.CONFIRMED);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                booking.setStatus(BookingStatus.CANCELLED);
            }
            payment.setGatewayTransactionId(params.get("vnp_TransactionNo"));
            paymentRepository.save(payment);
            bookingRepository.save(booking);
        }

        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public Map<String, String> verifyIpn(Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        try {
            String vnp_SecureHash = params.get("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            // Verify Signature
            Map<String, String> fields = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value != null && !value.isEmpty() && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                    fields.put(key, value);
                }
            }

            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            List<String> parts = new ArrayList<>();
            for (String fieldName : fieldNames) {
                String fieldValue = fields.get(fieldName);
                parts.add(encode(fieldName) + "=" + encode(fieldValue));
            }
            String hashData = String.join("&", parts);
            String computedHash = hmacSHA512(hashSecret, hashData);

            if (!computedHash.equalsIgnoreCase(vnp_SecureHash)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            String vnp_TxnRef = params.get("vnp_TxnRef");
            if (vnp_TxnRef == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
                return response;
            }

            Long paymentId = Long.parseLong(vnp_TxnRef);
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
                return response;
            }

            Payment payment = paymentOpt.get();

            // Verify Amount
            BigDecimal vnpAmount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));
            if (payment.getAmount().compareTo(vnpAmount) != 0) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return response;
            }

            // Verify status
            if (payment.getStatus() != PaymentStatus.PENDING) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            // Update status
            String responseCode = params.get("vnp_ResponseCode");
            Booking booking = payment.getBooking();
            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                booking.setStatus(BookingStatus.CONFIRMED);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                booking.setStatus(BookingStatus.CANCELLED);
            }
            payment.setGatewayTransactionId(params.get("vnp_TransactionNo"));
            paymentRepository.save(payment);
            bookingRepository.save(booking);

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;

        } catch (Exception e) {
            log.error("IPN handling failed", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            return response;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        // Chuẩn hóa địa chỉ IPv6 localhost hoặc các IPv6 khác thành IPv4 127.0.0.1 để tránh lỗi chữ ký VNPay
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || (ipAddress != null && ipAddress.contains(":"))) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
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
        } catch (Exception ex) {
            log.error("Error creating hmacSHA512 hash", ex);
            return "";
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBooking().getId())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentUrl(payment.getPaymentUrl())
                .build();
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.error("Error URL encoding value: {}", value, e);
            return "";
        }
    }
}
