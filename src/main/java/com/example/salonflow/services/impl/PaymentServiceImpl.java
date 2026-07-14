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
import com.example.salonflow.services.service.PaymentGatewayService;
import com.example.salonflow.services.service.PaymentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Qualifier("vnpayService")
    private final PaymentGatewayService vnpayService;

    @Qualifier("momoService")
    private final PaymentGatewayService momoService;

    @Qualifier("zalopayService")
    private final PaymentGatewayService zalopayService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentGatewayService getGatewayService(PaymentMethod method) {
        return switch (method) {
            case VNPAY -> vnpayService;
            case MOMO -> momoService;
            case ZALOPAY -> zalopayService;
        };
    }

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
            // Nếu vẫn đang PENDING và đã có paymentUrl, trả về luôn để tránh sinh lại giao dịch bên Gateway
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

        // Lưu trước để lấy ID (cần thiết cho mã app_trans_id của ZaloPay)
        payment = paymentRepository.saveAndFlush(payment);

        // Sinh link thanh toán từ gateway tương ứng
        PaymentGatewayService gatewayService = getGatewayService(request.getPaymentMethod());
        String paymentUrl = gatewayService.createPaymentUrl(payment, request.getReturnUrl());

        payment.setPaymentUrl(paymentUrl);
        payment = paymentRepository.save(payment);

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
    public void processVNPayWebhook(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || !vnpayService.verifyWebhookSignature(params, secureHash)) {
            throw new IllegalArgumentException("Chu ky VNPay khong hop le");
        }

        String idempotencyKey = params.get("vnp_TxnRef");
        // Lock bản ghi Payment bằng Pessimistic Write để tránh race condition
        Payment payment = paymentRepository.findByIdempotencyKeyWithLock(idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay giao dich voi key: " + idempotencyKey));

        // Tránh double processing nếu webhook bị gọi lại nhiều lần
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(transactionNo);
            
            // Cập nhật trạng thái Booking
            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayTransactionId(transactionNo);
        }

        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public void processMoMoWebhook(Map<String, String> params) {
        String signature = params.get("signature");
        if (signature == null || !momoService.verifyWebhookSignature(params, signature)) {
            throw new IllegalArgumentException("Chu ky MoMo khong hop le");
        }

        String idempotencyKey = params.get("orderId");
        Payment payment = paymentRepository.findByIdempotencyKeyWithLock(idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay giao dich voi key: " + idempotencyKey));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        String resultCode = params.get("resultCode");
        String transId = params.get("transId");

        if ("0".equals(resultCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(transId);

            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayTransactionId(transId);
        }

        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Map<String, Object> processZaloPayWebhook(Map<String, String> params) {
        String signature = params.get("mac");
        if (signature == null || !zalopayService.verifyWebhookSignature(params, signature)) {
            return Map.of("return_code", 2, "return_message", "Chu ky ZaloPay khong hop le");
        }

        try {
            String dataStr = params.get("data");
            Map<String, Object> dataMap = objectMapper.readValue(dataStr, new TypeReference<Map<String, Object>>() {});
            String appTransId = (String) dataMap.get("app_trans_id");
            
            // app_trans_id dang yyMMdd_paymentId, can tach lay paymentId
            String[] parts = appTransId.split("_");
            if (parts.length < 2) {
                return Map.of("return_code", 2, "return_message", "Format app_trans_id khong hop le");
            }
            
            Long paymentId = Long.parseLong(parts[1]);
            // Lock bản ghi để đảm bảo an toàn luồng dữ liệu
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay giao dich voi ID: " + paymentId));

            if (payment.getStatus() != PaymentStatus.PENDING) {
                return Map.of("return_code", 1, "return_message", "Giao dich da duoc xu ly truoc do");
            }

            String zpTransId = String.valueOf(dataMap.get("zp_trans_id"));
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(zpTransId);

            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            paymentRepository.save(payment);

            return Map.of("return_code", 1, "return_message", "success");
        } catch (Exception e) {
            return Map.of("return_code", 2, "return_message", e.getMessage());
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
}
