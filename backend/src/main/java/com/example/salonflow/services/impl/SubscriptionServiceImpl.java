package com.example.salonflow.services.impl;

import com.example.salonflow.config.properties.StripeProperties;
import com.example.salonflow.dto.subscription.ManualSubscriptionRequest;
import com.example.salonflow.dto.subscription.StripeCheckoutRequest;
import com.example.salonflow.dto.subscription.SubscriptionResponse;
import com.example.salonflow.dto.subscription.UpdateSubscriptionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.Subscription;
import com.example.salonflow.entity.SubscriptionFeatures;
import com.example.salonflow.entity.enums.BillingCycle;
import com.example.salonflow.entity.enums.SubscriptionPlan;
import com.example.salonflow.entity.enums.SubscriptionStatus;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.repository.StaffRepository;
import com.example.salonflow.repository.SubscriptionRepository;
import com.example.salonflow.services.service.EmailService;
import com.example.salonflow.services.service.SubscriptionService;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.example.salonflow.dto.payment.PaymentResponse;
import com.example.salonflow.entity.enums.PaymentMethod;
import com.example.salonflow.entity.enums.PaymentStatus;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;
    private final EmailService emailService;
    private final StripeProperties stripeProperties;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;


    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscription(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found with ID: " + salonId));

        List<Subscription> activeSubs = subscriptionRepository.findActiveSubscriptions(salonId, LocalDateTime.now());
        if (activeSubs.isEmpty()) {
            return mapToResponse(createDefaultFreeSubscription(salon));
        }
        return mapToResponse(activeSubs.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionFeatures getActiveFeatures(Long salonId) {
        List<Subscription> activeSubs = subscriptionRepository.findActiveSubscriptions(salonId, LocalDateTime.now());
        if (activeSubs.isEmpty()) {
            return SubscriptionFeatures.freeDefaults();
        }
        return activeSubs.get(0).getFeatures();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionHistory(Long salonId) {
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Salon not found with ID: " + salonId);
        }
        List<Subscription> history = subscriptionRepository.findBySalonIdOrderByCreatedAtDesc(salonId);
        List<SubscriptionResponse> responses = new ArrayList<>();
        for (Subscription sub : history) {
            responses.add(mapToResponse(sub));
        }
        return responses;
    }

    @Override
    @Transactional
    public String createStripeCheckoutSession(Long salonId, StripeCheckoutRequest request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found with ID: " + salonId));

        // 1. Calculate price
        BigDecimal price;
        if (request.getPlan() == SubscriptionPlan.PRO) {
            price = request.getBillingCycle() == BillingCycle.YEARLY ? BigDecimal.valueOf(4788000) : BigDecimal.valueOf(499000);
        } else if (request.getPlan() == SubscriptionPlan.ENTERPRISE) {
            price = request.getBillingCycle() == BillingCycle.YEARLY ? BigDecimal.valueOf(28800000) : BigDecimal.valueOf(3000000);
        } else {
            throw new BusinessException("Gói FREE không yêu cầu thanh toán.");
        }

        // 2. Create pending subscription record (PAST_DUE means pending payment)
        Subscription subscription = Subscription.builder()
                .salon(salon)
                .plan(request.getPlan())
                .features(getFeaturesForPlan(request.getPlan()))
                .billingCycle(request.getBillingCycle())
                .price(price)
                .status(SubscriptionStatus.PAST_DUE)
                .startDate(LocalDateTime.now())
                .endDate(calculateEndDate(LocalDateTime.now(), request.getBillingCycle()))
                .build();

        subscription = subscriptionRepository.save(subscription);

        // 3. Generate VNPay URL
        try {
            HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String clientIp = getClientIp(servletRequest);

            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String orderType = "other";
            long amount = price.multiply(new BigDecimal(100)).longValue();
            String vnp_TxnRef = "sub_" + subscription.getId();

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", tmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan goi cuoc " + request.getPlan().name() + " (" + request.getBillingCycle().name() + ")");
            vnp_Params.put("vnp_OrderType", orderType);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", request.getSuccessUrl());
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
            String vnp_SecureHash = hmacSHA512(hashSecret, hashData);
            String queryUrl = hashData + "&vnp_SecureHash=" + vnp_SecureHash;
            String generatedUrl = payUrl + "?" + queryUrl;

            log.info("Generated VNPay subscription URL: {} for subscription ID: {}", generatedUrl, subscription.getId());
            return generatedUrl;
        } catch (Exception e) {
            log.error("Error creating VNPay checkout session for subscription: ", e);
            throw new BusinessException("Failed to generate payment session: " + e.getMessage());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public String createStripePortalSession(Long salonId, String returnUrl) {
        List<Subscription> activeSubs = subscriptionRepository.findActiveSubscriptions(salonId, LocalDateTime.now());
        if (activeSubs.isEmpty() || stripeProperties.isMockEnable() || Stripe.apiKey == null) {
            return returnUrl;
        }

        Subscription active = activeSubs.get(0);
        if (active.getStripeCustomerId() == null) {
            return returnUrl;
        }

        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(active.getStripeCustomerId())
                            .setReturnUrl(returnUrl)
                            .build();
            com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
            return portalSession.getUrl();
        } catch (Exception e) {
            log.error("Error creating Stripe portal session: ", e);
            return returnUrl;
        }
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        if (stripeProperties.getWebhookSecret() == null || stripeProperties.getWebhookSecret().isEmpty()) {
            log.warn("Stripe Webhook Secret is not configured. Webhook event ignored.");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (Exception e) {
            log.error("Webhook signature verification failed: ", e);
            throw new BusinessException("Invalid webhook signature");
        }

        log.info("Received Stripe webhook event: {}", event.getType());
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        if (!dataObjectDeserializer.getObject().isPresent()) {
            throw new BusinessException("Webhook event data deserialization failed");
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) dataObjectDeserializer.getObject().get();
                handleCheckoutCompleted(session);
                break;
            case "customer.subscription.updated":
                com.stripe.model.Subscription stripeSubUpdated = (com.stripe.model.Subscription) dataObjectDeserializer.getObject().get();
                handleSubscriptionUpdated(stripeSubUpdated);
                break;
            case "customer.subscription.deleted":
                com.stripe.model.Subscription stripeSubDeleted = (com.stripe.model.Subscription) dataObjectDeserializer.getObject().get();
                handleSubscriptionDeleted(stripeSubDeleted);
                break;
            default:
                log.debug("Unhandled event type: {}", event.getType());
                break;
        }
    }

    private void handleCheckoutCompleted(Session session) {
        String stripeSubscriptionId = session.getSubscription();
        String stripeCustomerId = session.getCustomer();

        if (session.getMetadata() == null || session.getMetadata().get("salonId") == null) {
            log.warn("Checkout Session {} has no salonId metadata. Skipping.", session.getId());
            return;
        }

        Long salonId = Long.parseLong(session.getMetadata().get("salonId"));
        SubscriptionPlan plan = SubscriptionPlan.valueOf(session.getMetadata().get("plan"));
        BillingCycle billingCycle = BillingCycle.valueOf(session.getMetadata().get("billingCycle"));

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found with ID: " + salonId));

        expireActiveSubscriptions(salonId);

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(startDate, billingCycle);

        if (stripeSubscriptionId != null) {
            try {
                com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
                startDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart()), ZoneId.systemDefault());
                endDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()), ZoneId.systemDefault());
            } catch (Exception e) {
                log.error("Failed to retrieve subscription details from Stripe: ", e);
            }
        }

        BigDecimal price = BigDecimal.valueOf(session.getAmountTotal() != null ? session.getAmountTotal() / 100.0 : 0.0);

        Subscription subscription = Subscription.builder()
                .salon(salon)
                .plan(plan)
                .features(getFeaturesForPlan(plan))
                .billingCycle(billingCycle)
                .price(price)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .stripeSubscriptionId(stripeSubscriptionId)
                .stripeCustomerId(stripeCustomerId)
                .build();

        subscriptionRepository.save(subscription);
        log.info("Activated subscription {} (Plan: {}) for Salon {}", subscription.getId(), plan, salonId);
    }

    private void handleSubscriptionUpdated(com.stripe.model.Subscription stripeSub) {
        Optional<Subscription> localSubOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId());
        if (localSubOpt.isPresent()) {
            Subscription localSub = localSubOpt.get();
            localSub.setStartDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart()), ZoneId.systemDefault()));
            localSub.setEndDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()), ZoneId.systemDefault()));

            String stripeStatus = stripeSub.getStatus();
            if ("active".equals(stripeStatus)) {
                localSub.setStatus(SubscriptionStatus.ACTIVE);
            } else if ("past_due".equals(stripeStatus) || "unpaid".equals(stripeStatus)) {
                localSub.setStatus(SubscriptionStatus.PAST_DUE);
            } else if ("canceled".equals(stripeStatus)) {
                localSub.setStatus(SubscriptionStatus.CANCELED);
            }
            subscriptionRepository.save(localSub);
            log.info("Updated subscription status for stripe subscription: {}", stripeSub.getId());
        }
    }

    private void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSub) {
        Optional<Subscription> localSubOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId());
        if (localSubOpt.isPresent()) {
            Subscription localSub = localSubOpt.get();
            localSub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(localSub);
            log.info("Subscription marked as EXPIRED (stripe deleted): {}", stripeSub.getId());
        }
    }

    @Override
    @Transactional
    public SubscriptionResponse createManualSubscription(ManualSubscriptionRequest request) {
        Salon salon = salonRepository.findById(request.getSalonId())
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found with ID: " + request.getSalonId()));

        expireActiveSubscriptions(request.getSalonId());

        SubscriptionFeatures features = getFeaturesForPlan(request.getPlan());
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(request.getDurationDays());

        Subscription subscription = Subscription.builder()
                .salon(salon)
                .plan(request.getPlan())
                .features(features)
                .billingCycle(request.getBillingCycle())
                .price(request.getPrice())
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("Super Admin manually activated subscription {} (Plan: {}) for Salon {}", subscription.getId(), request.getPlan(), request.getSalonId());

        return mapToResponse(subscription);
    }

    @Override
    @Transactional
    public void checkExpiry() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expiredList = subscriptionRepository.findExpiredSubscriptions(now);

        for (Subscription sub : expiredList) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            log.info("Subscription ID {} for Salon {} has expired.", sub.getId(), sub.getSalon().getId());

            try {
                String ownerEmail = sub.getSalon().getOwner() != null ? sub.getSalon().getOwner().getEmail() : sub.getSalon().getEmail();
                if (ownerEmail != null) {
                    emailService.sendNotificationEmail(
                            ownerEmail,
                            "Gói đăng ký dịch vụ đã hết hạn - SalonFlow",
                            String.format("Chào bạn,<br/><br/>Gói dịch vụ <b>%s</b> của salon <b>%s</b> đã hết hạn vào ngày %s. Vui lòng gia hạn gói dịch vụ để tiếp tục sử dụng hệ thống.<br/><br/>Trân trọng,<br/>Đội ngũ SalonFlow.",
                                    sub.getPlan(), sub.getSalon().getName(), sub.getEndDate())
                    );
                }
            } catch (Exception e) {
                log.error("Failed to send expiry email for subscription ID: {}", sub.getId(), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBranchLimit(Long salonId) {
        SubscriptionFeatures features = getActiveFeatures(salonId);
        long currentBranches = branchRepository.findBySalonId(salonId).size();
        if (currentBranches >= features.getMaxBranches()) {
            throw new BusinessException(String.format("Giới hạn số lượng chi nhánh của bạn là %d. Bạn đã tạo %d chi nhánh. Vui lòng nâng cấp gói đăng ký để tạo thêm chi nhánh.",
                    features.getMaxBranches(), currentBranches));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateStaffLimit(Long salonId) {
        SubscriptionFeatures features = getActiveFeatures(salonId);
        long currentStaff = staffRepository.findByBranchSalonId(salonId).size();
        if (currentStaff >= features.getMaxStaff()) {
            throw new BusinessException(String.format("Giới hạn số lượng nhân viên của bạn là %d. Salon của bạn hiện có %d nhân viên. Vui lòng nâng cấp gói đăng ký để tuyển thêm nhân viên.",
                    features.getMaxStaff(), currentStaff));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAdvancedAnalytics(Long salonId) {
        SubscriptionFeatures features = getActiveFeatures(salonId);
        if (!features.isAnalyticsAdvanced()) {
            throw new BusinessException("Tính năng xem Báo cáo phân tích chuyên sâu yêu cầu gói đăng ký PRO hoặc ENTERPRISE.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAiFeatures(Long salonId) {
        SubscriptionFeatures features = getActiveFeatures(salonId);
        if (!features.isAiFeatures()) {
            throw new BusinessException("Tính năng thông minh AI yêu cầu gói đăng ký ENTERPRISE.");
        }
    }

    private void expireActiveSubscriptions(Long salonId) {
        List<Subscription> active = subscriptionRepository.findActiveSubscriptions(salonId, LocalDateTime.now());
        for (Subscription sub : active) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
        }
    }

    private SubscriptionFeatures getFeaturesForPlan(SubscriptionPlan plan) {
        switch (plan) {
            case PRO:
                return SubscriptionFeatures.proDefaults();
            case ENTERPRISE:
                return SubscriptionFeatures.enterpriseDefaults();
            default:
                return SubscriptionFeatures.freeDefaults();
        }
    }

    private LocalDateTime calculateEndDate(LocalDateTime start, BillingCycle cycle) {
        if (cycle == BillingCycle.MONTHLY) {
            return start.plusMonths(1);
        } else if (cycle == BillingCycle.YEARLY) {
            return start.plusYears(1);
        }
        return start.plusMonths(1); // Default to 1 month
    }

    private Subscription createDefaultFreeSubscription(Salon salon) {
        return Subscription.builder()
                .salon(salon)
                .plan(SubscriptionPlan.FREE)
                .features(SubscriptionFeatures.freeDefaults())
                .billingCycle(BillingCycle.MANUAL)
                .price(BigDecimal.ZERO)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(salon.getCreatedAt() != null ? LocalDateTime.ofInstant(salon.getCreatedAt(), ZoneId.systemDefault()) : LocalDateTime.now())
                .endDate(null) // Unlimited for FREE
                .build();
    }

    private SubscriptionResponse mapToResponse(Subscription sub) {
        return SubscriptionResponse.builder()
                .id(sub.getId())
                .salonId(sub.getSalon().getId())
                .salonName(sub.getSalon().getName())
                .plan(sub.getPlan())
                .features(sub.getFeatures())
                .billingCycle(sub.getBillingCycle())
                .price(sub.getPrice())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .stripeSubscriptionId(sub.getStripeSubscriptionId())
                .stripeCustomerId(sub.getStripeCustomerId())
                .createdAt(sub.getCreatedAt() != null ? LocalDateTime.ofInstant(sub.getCreatedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getAllSubscriptionsForAdmin(
            Long salonId,
            SubscriptionPlan plan,
            SubscriptionStatus status,
            Pageable pageable
    ) {
        Specification<Subscription> spec = (root, query, cb) -> cb.conjunction();
        
        if (salonId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("salon").get("id"), salonId));
        }
        if (plan != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("plan"), plan));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        
        return subscriptionRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public SubscriptionResponse updateSubscriptionForAdmin(Long id, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + id));

        if (request.getPlan() != null) {
            subscription.setPlan(request.getPlan());
        }
        if (request.getStatus() != null) {
            subscription.setStatus(request.getStatus());
        }
        if (request.getPrice() != null) {
            subscription.setPrice(request.getPrice());
        }
        if (request.getBillingCycle() != null) {
            subscription.setBillingCycle(request.getBillingCycle());
        }
        if (request.getStartDate() != null) {
            subscription.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            subscription.setEndDate(request.getEndDate());
        }

        SubscriptionFeatures features = subscription.getFeatures();
        if (features == null) {
            features = new SubscriptionFeatures();
        }
        boolean featuresUpdated = false;
        if (request.getMaxBranches() != null) {
            features.setMaxBranches(request.getMaxBranches());
            featuresUpdated = true;
        }
        if (request.getMaxStaff() != null) {
            features.setMaxStaff(request.getMaxStaff());
            featuresUpdated = true;
        }
        if (request.getAnalyticsAdvanced() != null) {
            features.setAnalyticsAdvanced(request.getAnalyticsAdvanced());
            featuresUpdated = true;
        }
        if (request.getAiFeatures() != null) {
            features.setAiFeatures(request.getAiFeatures());
            featuresUpdated = true;
        }
        if (featuresUpdated) {
            subscription.setFeatures(features);
        }

        subscription = subscriptionRepository.save(subscription);
        return mapToResponse(subscription);
    }

    @Override
    @Transactional
    public void cancelSubscriptionForAdmin(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + id));
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public com.example.salonflow.dto.payment.PaymentResponse verifySubscriptionPayment(java.util.Map<String, String> params) {
        String vnp_TxnRef = params.get("vnp_TxnRef");
        if (vnp_TxnRef == null || !vnp_TxnRef.startsWith("sub_")) {
            throw new IllegalArgumentException("Mã giao dịch không hợp lệ cho subscription");
        }

        Long subscriptionId = Long.parseLong(vnp_TxnRef.substring(4));
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Subscription ID: " + subscriptionId));

        BigDecimal vnpAmount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));
        if (subscription.getPrice().compareTo(vnpAmount) != 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không khớp");
        }

        String responseCode = params.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                expireActiveSubscriptions(subscription.getSalon().getId());
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setStartDate(LocalDateTime.now());
                subscription.setEndDate(calculateEndDate(LocalDateTime.now(), subscription.getBillingCycle()));
                subscription.setStripeSubscriptionId(params.get("vnp_TransactionNo")); // Lưu mã giao dịch cổng thanh toán
                subscription.setStripeCustomerId(params.get("vnp_BankCode")); // Lưu mã ngân hàng
                subscriptionRepository.save(subscription);
                log.info("Subscription ID {} activated successfully via VNPay callback.", subscriptionId);
            }
            return com.example.salonflow.dto.payment.PaymentResponse.builder()
                    .paymentId(subscriptionId)
                    .paymentMethod(com.example.salonflow.entity.enums.PaymentMethod.VNPAY)
                    .amount(subscription.getPrice())
                    .status(com.example.salonflow.entity.enums.PaymentStatus.SUCCESS)
                    .build();
        } else {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(subscription);
            log.warn("Subscription ID {} payment failed with VNPay response code: {}", subscriptionId, responseCode);
            return com.example.salonflow.dto.payment.PaymentResponse.builder()
                    .paymentId(subscriptionId)
                    .paymentMethod(com.example.salonflow.entity.enums.PaymentMethod.VNPAY)
                    .amount(subscription.getPrice())
                    .status(com.example.salonflow.entity.enums.PaymentStatus.FAILED)
                    .build();
        }
    }

    @Override
    @Transactional
    public java.util.Map<String, String> verifySubscriptionIpn(java.util.Map<String, String> params) {
        java.util.Map<String, String> response = new java.util.HashMap<>();
        try {
            String vnp_TxnRef = params.get("vnp_TxnRef");
            if (vnp_TxnRef == null || !vnp_TxnRef.startsWith("sub_")) {
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
                return response;
            }

            Long subscriptionId = Long.parseLong(vnp_TxnRef.substring(4));
            Optional<Subscription> subOpt = subscriptionRepository.findById(subscriptionId);
            if (subOpt.isEmpty()) {
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
                return response;
            }

            Subscription subscription = subOpt.get();

            BigDecimal vnpAmount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));
            if (subscription.getPrice().compareTo(vnpAmount) != 0) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return response;
            }

            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            String responseCode = params.get("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                expireActiveSubscriptions(subscription.getSalon().getId());
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setStartDate(LocalDateTime.now());
                subscription.setEndDate(calculateEndDate(LocalDateTime.now(), subscription.getBillingCycle()));
                subscription.setStripeSubscriptionId(params.get("vnp_TransactionNo"));
                subscription.setStripeCustomerId(params.get("vnp_BankCode"));
                subscriptionRepository.save(subscription);
                log.info("Subscription ID {} activated successfully via VNPay IPN.", subscriptionId);
            } else {
                subscription.setStatus(SubscriptionStatus.CANCELED);
                subscriptionRepository.save(subscription);
            }

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;
        } catch (Exception e) {
            log.error("Subscription IPN handling failed", e);
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

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.error("Error URL encoding value: {}", value, e);
            return "";
        }
    }
}
