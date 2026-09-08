package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.LockSlotRequest;
import com.example.salonflow.dto.booking.LockSlotResponse;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.Staff;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.StaffRepository;
import com.example.salonflow.services.service.SlotLockService;
import com.example.salonflow.websocket.BookingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/**
 * SlotLockServiceImpl
 *
 * Dùng Redis SETNX (SET if Not eXists) để lock slot:
 *   - Key:   "slot:{branchId}:{staffId}:{date}:{startTime}"
 *   - Value: holderId (userId hoặc guest:clientId)
 *   - TTL:   300 giây (5 phút)
 *
 * SETNX đảm bảo tính atomic — không có race condition khi 2 user cùng lock 1 slot.
 * Tích hợp WebSocket thông báo thời gian thực trạng thái SLOT_LOCKED / SLOT_UNLOCKED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotLockServiceImpl implements SlotLockService {

    private final StringRedisTemplate redisTemplate;
    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;
    private final BookingWebSocketHandler bookingWebSocketHandler;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.example.salonflow.pricing.BookingPricingService bookingPricingService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.example.salonflow.repository.ServiceBundleRepository serviceBundleRepository;

    private static final long LOCK_TTL_SECONDS = 300L; // 5 phút
    private static final String SLOT_KEY_PREFIX = "slot:";
    private static final String AVAILABILITY_CACHE_PREFIX = "availability:branch:";


    private int resolveDurationMinutes(LockSlotRequest request) {
        if (request.getDurationMinutes() != null && request.getDurationMinutes() > 0) {
            return request.getDurationMinutes();
        }
        if (request.getBundleId() != null && serviceBundleRepository != null && bookingPricingService != null) {
            try {
                var bundleOpt = serviceBundleRepository.findById(request.getBundleId());
                if (bundleOpt.isPresent()) {
                    var pricing = bookingPricingService.calculate(request.getBranchId(), null, bundleOpt.get());
                    if (pricing != null && pricing.getTotalDurationMinutes() > 0) {
                        return pricing.getTotalDurationMinutes();
                    }
                }
            } catch (Exception e) {
                log.warn("[SlotLock] Lỗi tính duration từ bundleId {}: {}", request.getBundleId(), e.getMessage());
            }
        }
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty() && bookingPricingService != null) {
            try {
                var pricing = bookingPricingService.calculate(request.getBranchId(), request.getServiceIds(), null);
                if (pricing != null && pricing.getTotalDurationMinutes() > 0) {
                    return pricing.getTotalDurationMinutes();
                }
            } catch (Exception e) {
                log.warn("[SlotLock] Lỗi tính duration từ serviceIds: {}", e.getMessage());
            }
        }
        if (request.getServiceId() != null && bookingPricingService != null) {
            try {
                var pricing = bookingPricingService.calculate(request.getBranchId(), List.of(request.getServiceId()), null);
                if (pricing != null && pricing.getTotalDurationMinutes() > 0) {
                    return pricing.getTotalDurationMinutes();
                }
            } catch (Exception e) {
                log.warn("[SlotLock] Lỗi tính duration từ serviceId: {}", e.getMessage());
            }
        }
        return 30; // Mặc định 30 phút nếu không có thông tin
    }

    private List<LocalTime> calculateIntermediateSlots(LocalTime startTime, int durationMinutes) {
        List<LocalTime> slots = new java.util.ArrayList<>();
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        LocalTime cur = startTime;
        while (cur.isBefore(endTime)) {
            slots.add(cur);
            cur = cur.plusMinutes(15);
        }
        return slots;
    }

    /**
     * Xóa toàn bộ cache availability liên quan đến branchId + date ngay lập tức.
     * Được gọi sau mọi lock / unlock để đảm bảo getAvailability không trả về dữ liệu cũ.
     */
    private void invalidateAvailabilityCache(Long branchId, String date) {
        try {
            String pattern = AVAILABILITY_CACHE_PREFIX + branchId + ":*:date:" + date + ":*";
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[SlotLock] Invalidated {} availability cache keys for branch={} date={}", keys.size(), branchId, date);
            }
        } catch (Exception ex) {
            log.warn("[SlotLock] Không thể invalidate availability cache: {}", ex.getMessage());
        }
    }

    private void releaseHolderPreviousLocks(String holderId, String previousSlotKey) {
        if (holderId != null && !holderId.isBlank()) {
            String activeKey = "holder:active:" + holderId;
            String existingKeys = redisTemplate.opsForValue().get(activeKey);
            if (existingKeys != null && !existingKeys.isBlank()) {
                for (String k : existingKeys.split(",")) {
                    if (!k.isBlank()) {
                        redisTemplate.delete(k);
                        broadcastUnlock(k);
                    }
                }
                redisTemplate.delete(activeKey);
            }
        }

        if (previousSlotKey != null && !previousSlotKey.isBlank()) {
            String currentHolder = redisTemplate.opsForValue().get(previousSlotKey);
            if (currentHolder != null && (holderId == null || holderId.equals(currentHolder))) {
                redisTemplate.delete(previousSlotKey);
                broadcastUnlock(previousSlotKey);
            }
        }
    }

    @Override
    public LockSlotResponse lockSlot(String holderId, LockSlotRequest request) {
        if (holderId == null || holderId.isBlank()) {
            holderId = (request.getClientId() != null && !request.getClientId().isBlank())
                    ? "guest:" + request.getClientId()
                    : "guest:unknown";
        }

        // Tự động hủy slot cũ nếu có
        releaseHolderPreviousLocks(holderId, request.getPreviousSlotKey());

        int durationMinutes = resolveDurationMinutes(request);
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        List<LocalTime> slotsToLock = calculateIntermediateSlots(startTime, durationMinutes);

        Long effectiveStaffId = request.getStaffId();
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);

        // Nếu khách không chỉ định thợ -> tìm thợ đủ điều kiện chưa bị đặt trong DB và chưa bị lock trong Redis ở bất kỳ slot nào
        if (effectiveStaffId == null) {
            List<Staff> branchStaff = staffRepository.findByBranchId(request.getBranchId());

            Staff freeStaff = null;
            for (Staff staff : branchStaff) {
                List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                        staff.getId(), request.getBookingDate(), startTime, endTime, activeStatuses);
                if (!overlaps.isEmpty()) {
                    continue;
                }

                boolean hasLockConflict = false;
                for (LocalTime t : slotsToLock) {
                    String checkKey = buildSlotKey(request.getBranchId(), staff.getId(), request.getBookingDate().toString(), t.toString());
                    String currentHolder = redisTemplate.opsForValue().get(checkKey);
                    if (currentHolder != null && !currentHolder.equals(holderId)) {
                        hasLockConflict = true;
                        break;
                    }
                }
                if (!hasLockConflict) {
                    freeStaff = staff;
                    break;
                }
            }

            if (freeStaff == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Khung giờ này hiện không còn nhân viên nào trống cho đủ thời lượng dịch vụ (" + durationMinutes + " phút). Vui lòng chọn khung giờ khác."
                );
            }
            effectiveStaffId = freeStaff.getId();
        } else {
            // Khách chọn thợ cụ thể -> kiểm tra trùng lịch DB và trùng khóa Redis trong toàn bộ khoảng [startTime, endTime)
            List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                    effectiveStaffId, request.getBookingDate(), startTime, endTime, activeStatuses);
            if (!overlaps.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Nhân viên đã có lịch hẹn khác trong khoảng thời gian thực hiện dịch vụ này."
                );
            }

            for (LocalTime t : slotsToLock) {
                String checkKey = buildSlotKey(request.getBranchId(), effectiveStaffId, request.getBookingDate().toString(), t.toString());
                String currentHolder = redisTemplate.opsForValue().get(checkKey);
                if (currentHolder != null && !currentHolder.equals(holderId)) {
                    Long ttl = redisTemplate.getExpire(checkKey);
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            String.format("Khung giờ %s đang có người chọn giữ chỗ (còn %ds). Vui lòng chọn khung giờ khác.", t.toString().substring(0, 5), ttl != null ? ttl : 300)
                    );
                }
            }
        }

        // Khóa TẤT CẢ các slot 15 phút từ startTime cho đến khi hoàn tất dịch vụ (endTime)
        List<String> lockedKeys = new java.util.ArrayList<>();
        for (LocalTime t : slotsToLock) {
            String key = buildSlotKey(
                    request.getBranchId(),
                    effectiveStaffId,
                    request.getBookingDate().toString(),
                    t.toString()
            );
            redisTemplate.opsForValue().set(key, holderId, Duration.ofSeconds(LOCK_TTL_SECONDS));
            lockedKeys.add(key);

            // Bắn WebSocket thông báo từng slot giờ đã bị lock để các client khác đổi sang MÀU VÀNG
            try {
                bookingWebSocketHandler.broadcastSlotUpdate(
                        request.getBranchId(),
                        effectiveStaffId,
                        request.getBookingDate().toString(),
                        "SLOT_LOCKED",
                        t.toString()
                );
            } catch (Exception ex) {
                log.error("[SlotLock] Lỗi broadcast WebSocket SLOT_LOCKED: {}", ex.getMessage());
            }
        }

        // Lưu danh sách các key đang giữ của holder này
        String activeKey = "holder:active:" + holderId;
        redisTemplate.opsForValue().set(activeKey, String.join(",", lockedKeys), Duration.ofSeconds(LOCK_TTL_SECONDS));

        // ✅ Invalidate availability cache ngay sau khi lock để UI các client khác thấy trạng thái mới nhất
        invalidateAvailabilityCache(request.getBranchId(), request.getBookingDate().toString());

        String primarySlotKey = buildSlotKey(
                request.getBranchId(),
                effectiveStaffId,
                request.getBookingDate().toString(),
                startTime.toString()
        );

        log.info("[SlotLock] Lock thành công: primaryKey={} allKeys={} holder={} duration={}p", primarySlotKey, lockedKeys, holderId, durationMinutes);

        return LockSlotResponse.builder()
                .slotKey(primarySlotKey)
                .ttlSeconds(LOCK_TTL_SECONDS)
                .assignedStaffId(effectiveStaffId)
                .message("Khung giờ và các khoảng thời gian dịch vụ (" + durationMinutes + " phút) đã được giữ cho bạn trong 5 phút. Vui lòng hoàn tất đặt lịch.")
                .build();
    }

    @Override
    public void unlockSlot(String holderId, String slotKey) {
        // Parse branchId và date từ slotKey trước khi delete để invalidate cache
        // Format: "slot:{branchId}:{staffId}:{date}:{startTime}" (date = yyyy-MM-dd, index 2 sau khi bỏ prefix)
        Long branchIdForCache = null;
        String dateForCache = null;
        if (slotKey != null && !slotKey.isBlank()) {
            try {
                // Bỏ prefix "slot:" rồi split: [branchId, staffId, yyyy-MM-dd, HH, mm, ss?]
                String withoutPrefix = slotKey.startsWith(SLOT_KEY_PREFIX)
                        ? slotKey.substring(SLOT_KEY_PREFIX.length())
                        : slotKey;
                String[] parts = withoutPrefix.split(":");
                if (parts.length >= 3) {
                    branchIdForCache = Long.parseLong(parts[0]);
                    // date là yyyy-MM-dd nằm ở index 2
                    dateForCache = parts[2];
                }
            } catch (Exception ignored) {}
        }
        releaseHolderPreviousLocks(holderId, slotKey);
        // ✅ Invalidate cache sau unlock
        if (branchIdForCache != null && dateForCache != null) {
            invalidateAvailabilityCache(branchIdForCache, dateForCache);
        }
    }

    @Override
    public void forceUnlock(String slotKey) {
        if (slotKey == null || slotKey.isBlank()) return;
        // Parse branchId và date để invalidate cache
        Long branchIdForCache = null;
        String dateForCache = null;
        try {
            String withoutPrefix = slotKey.startsWith(SLOT_KEY_PREFIX)
                    ? slotKey.substring(SLOT_KEY_PREFIX.length()) : slotKey;
            String[] parts = withoutPrefix.split(":");
            if (parts.length >= 3) {
                branchIdForCache = Long.parseLong(parts[0]);
                dateForCache = parts[2];
            }
        } catch (Exception ignored) {}

        String currentHolder = redisTemplate.opsForValue().get(slotKey);
        if (currentHolder != null) {
            releaseHolderPreviousLocks(currentHolder, slotKey);
        } else {
            redisTemplate.delete(slotKey);
            broadcastUnlock(slotKey);
        }
        // ✅ Invalidate cache sau force unlock
        if (branchIdForCache != null && dateForCache != null) {
            invalidateAvailabilityCache(branchIdForCache, dateForCache);
        }
    }

    private void broadcastUnlock(String slotKey) {
        try {
            // slotKey format: "slot:{branchId}:{staffId}:{date}:{startTime}"
            String[] parts = slotKey.replace(SLOT_KEY_PREFIX, "").split(":");
            if (parts.length >= 4) {
                Long branchId = Long.parseLong(parts[0]);
                Long staffId = Long.parseLong(parts[1]);
                String date = parts[2];
                String startTime = parts[3];

                bookingWebSocketHandler.broadcastSlotUpdate(
                        branchId,
                        staffId,
                        date,
                        "SLOT_UNLOCKED",
                        startTime
                );
            }
        } catch (Exception ex) {
            log.error("[SlotLock] Lỗi broadcast WebSocket SLOT_UNLOCKED: {}", ex.getMessage());
        }
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
