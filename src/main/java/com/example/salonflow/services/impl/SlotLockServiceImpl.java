package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.LockSlotRequest;
import com.example.salonflow.dto.booking.LockSlotResponse;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.services.service.SlotLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * SlotLockServiceImpl
 *
 * Dùng Redis SETNX (SET if Not eXists) để lock slot:
 *   - Key:   "slot:{branchId}:{staffId}:{date}:{startTime}"
 *   - Value: customerId (để biết ai đang giữ lock)
 *   - TTL:   600 giây (10 phút)
 *
 * SETNX đảm bảo tính atomic — không có race condition
 * khi 2 user cùng lock 1 slot cùng lúc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotLockServiceImpl implements SlotLockService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;

    private static final long LOCK_TTL_SECONDS = 600L; // 10 phút
    private static final String SLOT_KEY_PREFIX = "slot:";

    @Override
    public LockSlotResponse lockSlot(Long customerId, LockSlotRequest request) {

        // Validate customer tồn tại
        userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + customerId));

        // Validate service tồn tại
        serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found: " + request.getServiceId()));

        // Build slot key
        String slotKey = buildSlotKey(
                request.getBranchId(),
                request.getStaffId(),
                request.getBookingDate().toString(),
                request.getStartTime().toString()
        );

        // ── Redis SETNX (atomic) ──────────────────────────────
        // setIfAbsent = SET key value EX ttl NX
        // Trả về true nếu set thành công (slot chưa bị lock)
        // Trả về false nếu key đã tồn tại (slot đã bị lock)
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                slotKey,
                String.valueOf(customerId),
                Duration.ofSeconds(LOCK_TTL_SECONDS)
        );

        if (Boolean.FALSE.equals(locked)) {
            // Slot đã bị lock bởi user khác
            Long ttl = redisTemplate.getExpire(slotKey);
            log.warn("[SlotLock] Slot đã bị lock: key={} ttl={}s", slotKey, ttl);

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Slot này đang được giữ bởi người khác. Vui lòng chọn slot khác."
            );
        }

        log.info("[SlotLock] Lock thành công: key={} customerId={}", slotKey, customerId);

        return LockSlotResponse.builder()
                .slotKey(slotKey)
                .ttlSeconds(LOCK_TTL_SECONDS)
                .message("Slot đã được giữ trong 10 phút. Vui lòng hoàn tất đặt lịch.")
                .build();
    }

    @Override
    public void unlockSlot(Long customerId, String slotKey) {

        String currentHolder = redisTemplate.opsForValue().get(slotKey);

        if (currentHolder == null) {
            // Slot đã hết TTL hoặc không tồn tại — không làm gì
            log.info("[SlotLock] Unlock: key={} không tồn tại (đã hết TTL)", slotKey);
            return;
        }

        // Chỉ user đang giữ lock mới được unlock
        if (!currentHolder.equals(String.valueOf(customerId))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hủy lock slot này."
            );
        }

        redisTemplate.delete(slotKey);
        log.info("[SlotLock] Unlock thành công: key={} customerId={}", slotKey, customerId);
    }

    @Override
    public boolean isSlotLocked(String slotKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
    }

    @Override
    public Long getSlotTtl(String slotKey) {
        Long ttl = redisTemplate.getExpire(slotKey);
        return ttl != null ? ttl : -1L;
    }

    // ── Helper ────────────────────────────────────────────────

    /**
     * Build slot key từ các thành phần.
     * Format: "slot:{branchId}:{staffId}:{date}:{startTime}"
     * VD: "slot:1:5:2026-06-30:09:00"
     */
    public static String buildSlotKey(
            Long branchId,
            Long staffId,
            String date,
            String startTime
    ) {
        return SLOT_KEY_PREFIX + branchId + ":" + staffId + ":" + date + ":" + startTime;
    }
}
