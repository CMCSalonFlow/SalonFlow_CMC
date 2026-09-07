package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.LockSlotRequest;
import com.example.salonflow.dto.booking.LockSlotResponse;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.SalonService;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.repository.StaffRepository;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.websocket.BookingWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho SlotLockServiceImpl.
 * Dùng Mockito mock Redis và Repository — không cần DB hay Redis thật.
 */
@ExtendWith(MockitoExtension.class)
class SlotLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingWebSocketHandler bookingWebSocketHandler;

    private SlotLockServiceImpl slotLockService;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long STAFF_ID    = 5L;
    private static final Long BRANCH_ID   = 1L;
    private static final Long SERVICE_ID  = 3L;

    private LockSlotRequest buildRequest() {
        LockSlotRequest req = new LockSlotRequest();
        req.setBranchId(BRANCH_ID);
        req.setStaffId(STAFF_ID);
        req.setServiceId(SERVICE_ID);
        req.setBookingDate(LocalDate.of(2026, 6, 30));
        req.setStartTime(LocalTime.of(9, 0));
        return req;
    }

    @BeforeEach
    void setUp() {
        slotLockService = new SlotLockServiceImpl(
                redisTemplate,
                staffRepository,
                bookingRepository,
                bookingWebSocketHandler
        );
    }

    // ── lockSlot ────────────────────────────────────────────────

    @Test
    @DisplayName("✅ Lock slot thành công → trả về slotKey và ttl 300s")
    void lockSlot_success() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Act
        LockSlotResponse response = slotLockService.lockSlot(
                CUSTOMER_ID, buildRequest());

        // Assert
        assertThat(response.getSlotKey()).isEqualTo(
                "slot:1:5:2026-06-30:09:00");
        assertThat(response.getTtlSeconds()).isEqualTo(300L);
        assertThat(response.getMessage()).contains("5 phút");

        // Verify Redis set được gọi để lock slot
        verify(valueOps).set(
                eq("slot:1:5:2026-06-30:09:00"),
                eq("user:" + CUSTOMER_ID),
                eq(Duration.ofSeconds(300))
        );
    }

    @Test
    @DisplayName("🚫 Lock slot đã bị lock bởi người khác → 409 Conflict")
    void lockSlot_alreadyLocked_returns409() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString()))
                .thenReturn("user:999"); // người khác đang giữ
        when(redisTemplate.getExpire(anyString()))
                .thenReturn(240L);

        // Act & Assert
        assertThatThrownBy(() ->
                slotLockService.lockSlot(CUSTOMER_ID, buildRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("người chọn");
                });
    }

    // ── unlockSlot ──────────────────────────────────────────────

    @Test
    @DisplayName("✅ Unlock slot thành công — đúng owner")
    void unlockSlot_success() {
        String slotKey = "slot:1:5:2026-06-30:09:00";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("holder:active:user:" + CUSTOMER_ID)).thenReturn(slotKey);

        slotLockService.unlockSlot(CUSTOMER_ID, slotKey);

        verify(redisTemplate, atLeastOnce()).delete(slotKey);
    }

    @Test
    @DisplayName("✅ Unlock slot đã hết TTL → không lỗi")
    void unlockSlot_expiredKey_noError() {
        String slotKey = "slot:1:5:2026-06-30:09:00";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("holder:active:user:" + CUSTOMER_ID)).thenReturn(null);
        when(valueOps.get(slotKey)).thenReturn(null); // Key không tồn tại

        slotLockService.unlockSlot(CUSTOMER_ID, slotKey);

        verify(redisTemplate, never()).delete(slotKey);
    }

    @Test
    @DisplayName("🚫 Unlock slot của người khác → Bị từ chối, không delete")
    void unlockSlot_notOwner_doesNotDelete() {
        String slotKey = "slot:1:5:2026-06-30:09:00";
        Long otherCustomerId = 999L;

        // Slot đang bị lock bởi otherCustomerId
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("holder:active:user:" + CUSTOMER_ID)).thenReturn(null);
        when(valueOps.get(slotKey))
                .thenReturn("user:" + otherCustomerId);

        slotLockService.unlockSlot(CUSTOMER_ID, slotKey);

        verify(redisTemplate, never()).delete(slotKey);
    }

    // ── isSlotLocked & getSlotTtl ───────────────────────────────

    @Test
    @DisplayName("✅ isSlotLocked = true khi key tồn tại")
    void isSlotLocked_keyExists_returnsTrue() {
        String slotKey = "slot:1:5:2026-06-30:09:00";
        when(redisTemplate.hasKey(slotKey)).thenReturn(true);

        assertThat(slotLockService.isSlotLocked(slotKey)).isTrue();
    }

    @Test
    @DisplayName("✅ isSlotLocked = false khi key không tồn tại")
    void isSlotLocked_keyNotExists_returnsFalse() {
        String slotKey = "slot:1:5:2026-06-30:09:00";
        when(redisTemplate.hasKey(slotKey)).thenReturn(false);

        assertThat(slotLockService.isSlotLocked(slotKey)).isFalse();
    }

    @Test
    @DisplayName("✅ getSlotTtl trả về TTL còn lại")
    void getSlotTtl_returnsRemainingTime() {
        String slotKey = "slot:1:5:2026-06-30:09:00";
        when(redisTemplate.getExpire(slotKey)).thenReturn(540L);

        assertThat(slotLockService.getSlotTtl(slotKey)).isEqualTo(540L);
    }

    @Test
    @DisplayName("✅ getSlotTtl trả về -1 khi key không tồn tại")
    void getSlotTtl_keyNotExists_returnsMinusOne() {
        String slotKey = "slot:1:5:2026-06-30:09:00";
        when(redisTemplate.getExpire(slotKey)).thenReturn(null);

        assertThat(slotLockService.getSlotTtl(slotKey)).isEqualTo(-1L);
    }

    // ── buildSlotKey ────────────────────────────────────────────

    @Test
    @DisplayName("✅ buildSlotKey tạo đúng format")
    void buildSlotKey_correctFormat() {
        String key = SlotLockServiceImpl.buildSlotKey(1L, 5L, "2026-06-30", "09:00");
        assertThat(key).isEqualTo("slot:1:5:2026-06-30:09:00");
    }
}