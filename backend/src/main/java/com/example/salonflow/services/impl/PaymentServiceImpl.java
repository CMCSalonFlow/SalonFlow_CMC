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

    @Value("${sepay.secret-key:}")
    private String sepaySecretKey;


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



    private BigDecimal resolvePaymentAmount(Booking booking) {
        BigDecimal depositAmount = booking.getDepositAmount();
        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            return depositAmount;
        }
        return booking.getTotalPrice();
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
            if (booking.getStatus() == BookingStatus.COMPLETED) {
                emailService.sendInvoiceEmail(booking, invoiceUrl);
            }
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

        if (content.toUpperCase().contains("SUB")) {
            try {
                String numericPart = content.replaceAll("[^0-9]", "");
                if (!numericPart.isEmpty()) {
                    Long subId = Long.parseLong(numericPart);
                    subscriptionService.activateSubscriptionViaBankTransfer(subId);
                    log.info("Xác nhận tự động gói dịch vụ ID {} qua SePay Webhook.", subId);
                    return PaymentResponse.builder()
                            .paymentId(subId)
                            .paymentMethod(PaymentMethod.BANK_TRANSFER)
                            .status(PaymentStatus.SUCCESS)
                            .build();
                }
            } catch (Exception e) {
                log.error("Lỗi parse subscription ID từ webhook content: {}", content, e);
            }
        }

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

        throw new IllegalArgumentException("Không thể tìm thấy Booking ID hoặc Subscription ID từ nội dung Webhook");
    }
}
