package com.example.salonflow.services.impl;

import com.example.salonflow.dto.audit.CreateAuditLogRequest;
import com.example.salonflow.dto.payment.CreatePaymentUrlRequest;
import com.example.salonflow.dto.payment.PaymentResponse;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Payment;
import com.example.salonflow.entity.enums.AuditAction;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.PaymentMethod;
import com.example.salonflow.entity.enums.PaymentStatus;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.PaymentRepository;
import com.example.salonflow.services.service.AuditLogService;
import com.example.salonflow.services.service.PaymentService;
import com.example.salonflow.services.service.EmailService;
import com.example.salonflow.services.service.InvoicePdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final InvoicePdfService invoicePdfService;
    private final EmailService emailService;
    private final com.example.salonflow.services.service.SubscriptionService subscriptionService;
    private final AuditLogService auditLogService; // thêm

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.api-url}")
    private String apiUrl;

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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Khong tim thay lich hen voi ID: " + request.getBookingId()));

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Khong the thanh toan cho lich hen o trang thai: " + booking.getStatus());
        }

        boolean alreadyPaid = paymentRepository
                .findByBookingId(booking.getId())
                .stream()
                .anyMatch(payment -> payment.getPaymentMethod() == request.getPaymentMethod()
                        && (payment.getStatus() == PaymentStatus.SUCCESS
                                || payment.getStatus() == PaymentStatus.REFUNDED));
        if (alreadyPaid) {
            throw new IllegalStateException("Lich hen nay da duoc thanh toan thanh cong");
        }

        BigDecimal paymentAmount = resolvePaymentAmount(booking);

        // Tạo bản ghi Payment ở trạng thái PENDING
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(request.getPaymentMethod())
                .amount(paymentAmount)
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        payment = paymentRepository.save(payment);

        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            try {
                HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder
                        .currentRequestAttributes()).getRequest();
                String clientIp = getClientIp(servletRequest);

                String vnp_Version = "2.1.0";
                String vnp_Command = "pay";
                String orderType = "other";
                long amount = paymentAmount.multiply(new BigDecimal(100)).longValue();
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Khong tim thay giao dich thanh toan cho Booking ID: " + bookingId));
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
            if (value != null && !value.isEmpty() && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType")) {
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

        if (vnp_TxnRef.startsWith("sub_")) {
            return subscriptionService.verifySubscriptionPayment(params);
        }

        Long paymentId = Long.parseLong(vnp_TxnRef);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Giao dich khong ton tai: " + paymentId));

        // Check amount
        BigDecimal vnpAmount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));
        if (payment.getAmount().compareTo(vnpAmount) != 0) {
            throw new IllegalArgumentException("So tien khong khop");
        }

        Booking booking = payment.getBooking();

        // Check order status
        if (payment.getStatus() == PaymentStatus.PENDING) {
            String responseCode = params.get("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                booking.setStatus(BookingStatus.CONFIRMED);

                try {
                    String invoiceUrl = invoicePdfService.generateInvoice(booking);
                    booking.setInvoiceUrl(invoiceUrl);
                    emailService.sendInvoiceEmail(booking, invoiceUrl);
                    log.info("Invoice created: {}", invoiceUrl);
                } catch (Exception ex) {
                    log.error("Generate invoice failed", ex);
                }

            } else {
                payment.setStatus(PaymentStatus.FAILED);
                booking.setStatus(BookingStatus.CANCELLED);
            }
            payment.setGatewayTransactionId(params.get("vnp_TransactionNo"));
            payment.setGatewayTransactionDate(extractGatewayTransactionDate(params));
            booking = bookingRepository.save(booking);
            payment.setBooking(booking);
            payment = paymentRepository.save(payment);
        } else {
            if (booking != null) {
                booking = bookingRepository.findById(booking.getId()).orElse(booking);
                payment.setBooking(booking);
            }
        }

        PaymentResponse response = mapToResponse(payment);
        if (booking != null && booking.getInvoiceUrl() != null) {
            response.setInvoiceUrl(booking.getInvoiceUrl());
        }
        return response;
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
                if (value != null && !value.isEmpty() && !key.equals("vnp_SecureHash")
                        && !key.equals("vnp_SecureHashType")) {
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

            if (vnp_TxnRef.startsWith("sub_")) {
                return subscriptionService.verifySubscriptionIpn(params);
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

                try {
                    String invoiceUrl = invoicePdfService.generateInvoice(booking);
                    booking.setInvoiceUrl(invoiceUrl);
                    emailService.sendInvoiceEmail(booking, invoiceUrl);
                    log.info("Invoice created: {}", invoiceUrl);
                } catch (Exception ex) {
                    log.error("Generate invoice failed", ex);
                }

            } else {
                payment.setStatus(PaymentStatus.FAILED);
                booking.setStatus(BookingStatus.CANCELLED);
            }
            payment.setGatewayTransactionId(params.get("vnp_TransactionNo"));
            payment.setGatewayTransactionDate(extractGatewayTransactionDate(params));
            booking = bookingRepository.save(booking);
            payment.setBooking(booking);
            paymentRepository.save(payment);

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
        String invoiceUrl = null;
        Long bookingId = null;
        if (payment.getBooking() != null) {
            bookingId = payment.getBooking().getId();
            invoiceUrl = payment.getBooking().getInvoiceUrl();
        }

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(bookingId)
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentUrl(payment.getPaymentUrl())
                .refundAmount(payment.getRefundAmount())
                .refundTransactionId(payment.getRefundTransactionId())
                .invoiceUrl(invoiceUrl)
                .refundedAt(payment.getRefundedAt())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse refundDeposit(Long bookingId, BigDecimal refundAmount, String reason) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("So tien hoan phai lon hon 0");
        }

        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("Chua cau hinh VNPay refund API URL");
        }

        Payment payment = paymentRepository
                .findFirstByBookingIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                        bookingId,
                        PaymentMethod.VNPAY,
                        PaymentStatus.SUCCESS)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Khong tim thay giao dich VNPay da thanh toan cho booking " + bookingId));

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("So tien hoan khong duoc lon hon so tien da thanh toan");
        }

        if (payment.getRefundedAt() != null || payment.getStatus() == PaymentStatus.REFUNDED) {
            return mapToResponse(payment);
        }

        String originalTransactionDate = payment.getGatewayTransactionDate();
        if (originalTransactionDate == null || originalTransactionDate.isBlank()) {
            throw new IllegalStateException("Khong co ngay giao dich goc de hoan tien VNPay");
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = currentVnpayTime();
        String txnRef = payment.getId().toString();
        String transactionNo = payment.getGatewayTransactionId();
        if (transactionNo == null || transactionNo.isBlank()) {
            throw new IllegalStateException("Khong co ma giao dich VNPay de hoan tien");
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("vnp_RequestId", requestId);
        payload.put("vnp_Version", "2.1.0");
        payload.put("vnp_Command", "refund");
        payload.put("vnp_TmnCode", tmnCode);
        payload.put("vnp_TransactionType", "02");
        payload.put("vnp_TxnRef", txnRef);
        payload.put("vnp_Amount",
                refundAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString());
        payload.put("vnp_OrderInfo", buildRefundOrderInfo(payment, reason));
        payload.put("vnp_TransactionNo", transactionNo);
        payload.put("vnp_TransactionDate", originalTransactionDate);
        payload.put("vnp_CreateDate", createDate);
        payload.put("vnp_CreateBy", "salonflow");
        payload.put("vnp_IpAddr", "127.0.0.1");

        Map<String, String> signPayload = new LinkedHashMap<>(payload);
        String hashData = buildSignedData(signPayload);
        payload.put("vnp_SecureHash", hmacSHA512(hashSecret, hashData));

        try {
            Map<?, ?> response = WebClient.create()
                    .post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("VNPay refund tra ve response rong");
            }

            String responseCode = firstNonBlank(
                    stringValue(response.get("vnp_ResponseCode")),
                    stringValue(response.get("RspCode")),
                    stringValue(response.get("ResponseCode")));

            if (!"00".equals(responseCode)) {
                String message = firstNonBlank(
                        stringValue(response.get("vnp_Message")),
                        stringValue(response.get("Message")),
                        "VNPay refund failed");
                throw new IllegalStateException(message);
            }

            payment.setRefundAmount(refundAmount);
            payment.setRefundTransactionId(firstNonBlank(
                    stringValue(response.get("vnp_TransactionNo")),
                    stringValue(response.get("TransactionNo")),
                    requestId));
            payment.setRefundedAt(Instant.now());
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            // Cách A: ghi audit log nghiệp vụ nhạy cảm — refund
            auditLogService.log(CreateAuditLogRequest.builder()
                    .action(AuditAction.REFUND)
                    .resourceType("Payment")
                    .resourceId(String.valueOf(payment.getId()))
                    .oldValue("status=SUCCESS, amount=" + payment.getAmount())
                    .newValue("status=REFUNDED, refundAmount=" + refundAmount + ", reason=" + reason)
                    .build());

            log.info("Refunded VNPay payment {} for booking {} with amount {}", payment.getId(), bookingId,
                    refundAmount);
            return mapToResponse(payment);
        } catch (Exception e) {
            log.error("Refund VNPay failed for booking {}: {}", bookingId, e.getMessage(), e);
            throw new IllegalStateException("Khong the hoan tien VNPay: " + e.getMessage());
        }
    }

    private BigDecimal resolvePaymentAmount(Booking booking) {
        BigDecimal depositAmount = booking.getDepositAmount();
        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            return depositAmount;
        }
        return booking.getTotalPrice();
    }

    private String extractGatewayTransactionDate(Map<String, String> params) {
        return firstNonBlank(params.get("vnp_PayDate"), params.get("vnp_TransactionDate"));
    }

    private String currentVnpayTime() {
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(cld.getTime());
    }

    private String buildRefundOrderInfo(Payment payment, String reason) {
        String sanitizedReason = reason == null ? "" : reason.trim();
        if (sanitizedReason.isEmpty()) {
            sanitizedReason = "Hoan tien booking";
        }
        return "Hoan tien booking " + payment.getBooking().getId() + " - " + sanitizedReason;
    }

    private String buildSignedData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        List<String> parts = new ArrayList<>();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                parts.add(encode(fieldName) + "=" + encode(fieldValue));
            }
        }
        return String.join("&", parts);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.error("Error URL encoding value: {}", value, e);
            return "";
        }
    }

    @Override
    @Transactional
    public PaymentResponse processPosCashPayment(com.example.salonflow.dto.payment.PosCashPaymentRequest request) {
        Long staffId = com.example.salonflow.security.SecurityUtils.getCurrentUserId();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new com.example.salonflow.exception.ResourceNotFoundException(
                        "Không tìm thấy lịch hẹn ID: " + request.getBookingId()));

        BigDecimal cashAmount = request.getAmount() != null ? request.getAmount() : booking.getTotalPrice();

        java.util.Optional<Payment> existingOpt = paymentRepository.findByBookingId(booking.getId())
                .stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CASH && p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst();

        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            return PaymentResponse.builder()
                    .paymentId(existing.getId())
                    .bookingId(booking.getId())
                    .paymentMethod(PaymentMethod.CASH)
                    .amount(existing.getAmount())
                    .status(PaymentStatus.SUCCESS)
                    .confirmedBy(existing.getConfirmedBy())
                    .invoiceUrl(booking.getInvoiceUrl())
                    .build();
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.CASH)
                .amount(cashAmount)
                .status(PaymentStatus.SUCCESS)
                .confirmedBy(staffId)
                .idempotencyKey("pos_cash_" + booking.getId() + "_" + System.currentTimeMillis())
                .gatewayTransactionId("CASH_POS_" + System.currentTimeMillis())
                .build();

        payment = paymentRepository.save(payment);

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking = bookingRepository.save(booking);
        }

        try {
            String invoiceUrl = invoicePdfService.generateInvoice(booking);
            booking.setInvoiceUrl(invoiceUrl);
            emailService.sendInvoiceEmail(booking, invoiceUrl);
            log.info("Invoice created for POS cash payment: {}", invoiceUrl);
        } catch (Exception ex) {
            log.error("Generate invoice failed for POS cash payment", ex);
        }

        log.info("Xác nhận thanh toán tiền mặt POS thành công cho Booking ID: {}, Số tiền: {}, Staff ID xác nhận: {}",
                booking.getId(), cashAmount, staffId);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(booking.getId())
                .paymentMethod(PaymentMethod.CASH)
                .amount(payment.getAmount())
                .status(PaymentStatus.SUCCESS)
                .confirmedBy(staffId)
                .invoiceUrl(booking.getInvoiceUrl())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse autoConfirmBankTransfer(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Booking ID: " + bookingId));

        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        BigDecimal amount = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .idempotencyKey("vietqr_auto_" + booking.getId() + "_" + System.currentTimeMillis())
                .gatewayTransactionId("VIETQR_" + System.currentTimeMillis())
                .build();

        payment = paymentRepository.save(payment);

        try {
            String invoiceUrl = invoicePdfService.generateInvoice(booking);
            booking.setInvoiceUrl(invoiceUrl);
            emailService.sendInvoiceEmail(booking, invoiceUrl);
        } catch (Exception ex) {
            log.error("Failed to generate invoice during auto confirm", ex);
        }

        log.info("Tự động xác nhận thanh toán Chuyển khoản VietQR cho Booking ID: {}", bookingId);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(booking.getId())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(payment.getAmount())
                .status(PaymentStatus.SUCCESS)
                .invoiceUrl(booking.getInvoiceUrl())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse processSepayWebhook(Map<String, Object> payload) {
        log.info("Nhận Webhook SePay / VietQR: {}", payload);
        String content = payload != null ? String.valueOf(payload.getOrDefault("content", "")) : "";
        Long bookingId = null;

        if (content.contains("SF")) {
            try {
                String numericPart = content.replaceAll("[^0-9]", "");
                if (!numericPart.isEmpty()) {
                    bookingId = Long.parseLong(numericPart);
                }
            } catch (Exception e) {
                log.error("Lỗi parse booking ID từ webhook content: {}", content, e);
            }
        }

        if (bookingId == null && payload != null && payload.containsKey("bookingId")) {
            try {
                bookingId = Long.parseLong(payload.get("bookingId").toString());
            } catch (Exception e) {
            }
        }

        if (bookingId != null) {
            return autoConfirmBankTransfer(bookingId);
        }

        throw new IllegalArgumentException("Không thể tìm thấy Booking ID từ nội dung Webhook");
    }
}
