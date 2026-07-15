package com.example.salonflow.services.impl;

import com.example.salonflow.dto.recurring.RecurringBookingRequest;
import com.example.salonflow.dto.recurring.RecurringBookingPreviewResponse;
import com.example.salonflow.entity.*;
import com.example.salonflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho RecurringBookingServiceImpl.
 * Tập trung vào: generate ngày lặp đúng pattern, detect conflict,
 * validate giới hạn MAX_OCCURRENCES.
 */
@ExtendWith(MockitoExtension.class)
class RecurringBookingServiceTest {

    @Mock private RecurringBookingRepository recurringBookingRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private StaffRepository staffRepository;

    private RecurringBookingServiceImpl service;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long STAFF_ID = 5L;
    private static final Long BRANCH_ID = 1L;
    private static final Long SERVICE_ID = 3L;

    @BeforeEach
    void setUp() {
        service = new RecurringBookingServiceImpl(
                recurringBookingRepository,
                bookingRepository,
                userRepository,
                serviceRepository,
                branchRepository,
                staffRepository
        );
    }

    private RecurringBookingRequest buildRequest(
            String pattern, LocalDate start, LocalDate end
    ) {
        RecurringBookingRequest req = new RecurringBookingRequest();
        req.setBranchId(BRANCH_ID);
        req.setStaffId(STAFF_ID);
        req.setServiceId(SERVICE_ID);
        req.setPattern(pattern);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setStartTime(LocalTime.of(9, 0));
        req.setEndTime(LocalTime.of(10, 0));
        return req;
    }

    // ── preview: WEEKLY ─────────────────────────────────────────

    @Test
    @DisplayName("✅ Preview WEEKLY 2 tháng → đúng 9 ngày, cách nhau 7 ngày")
    void preview_weekly_generatesCorrectDates() {
        RecurringBookingRequest req = buildRequest(
                "WEEKLY",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 1)
        );

        when(bookingRepository.existsConflict(
                anyLong(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);

        RecurringBookingPreviewResponse response = service.preview(CUSTOMER_ID, req);

        assertThat(response.getTotalOccurrences()).isEqualTo(9);
        assertThat(response.getConflictCount()).isEqualTo(0);
        assertThat(response.getOkCount()).isEqualTo(9);

        // Kiểm tra ngày đầu và cách nhau đúng 7 ngày
        assertThat(response.getOccurrences().get(0).getDate())
                .isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getOccurrences().get(1).getDate())
                .isEqualTo(LocalDate.of(2026, 7, 8));
        assertThat(response.getOccurrences().get(8).getDate())
                .isEqualTo(LocalDate.of(2026, 8, 26));
    }

    @Test
    @DisplayName("✅ Preview BIWEEKLY → cách nhau đúng 14 ngày")
    void preview_biweekly_generatesCorrectDates() {
        RecurringBookingRequest req = buildRequest(
                "BIWEEKLY",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 1)
        );

        when(bookingRepository.existsConflict(
                anyLong(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);

        RecurringBookingPreviewResponse response = service.preview(CUSTOMER_ID, req);

        assertThat(response.getTotalOccurrences()).isEqualTo(5);
        assertThat(response.getOccurrences().get(0).getDate())
                .isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getOccurrences().get(1).getDate())
                .isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    @DisplayName("🚫 Preview phát hiện conflict đúng ngày")
    void preview_detectsConflictCorrectly() {
        RecurringBookingRequest req = buildRequest(
                "WEEKLY",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 22) // 4 occurrences: 1, 8, 15, 22/7
        );

        // Giả lập ngày 15/7 bị conflict, các ngày khác OK
        when(bookingRepository.existsConflict(
                eq(STAFF_ID),
                eq(LocalDate.of(2026, 7, 15)),
                any(), any()))
                .thenReturn(true);
        when(bookingRepository.existsConflict(
                eq(STAFF_ID),
                argThat(d -> !d.equals(LocalDate.of(2026, 7, 15))),
                any(), any()))
                .thenReturn(false);

        RecurringBookingPreviewResponse response = service.preview(CUSTOMER_ID, req);

        assertThat(response.getTotalOccurrences()).isEqualTo(4);
        assertThat(response.getConflictCount()).isEqualTo(1);
        assertThat(response.getOkCount()).isEqualTo(3);

        var conflictDay = response.getOccurrences().stream()
                .filter(o -> o.getDate().equals(LocalDate.of(2026, 7, 15)))
                .findFirst().orElseThrow();
        assertThat(conflictDay.isHasConflict()).isTrue();
        assertThat(conflictDay.getConflictReason()).isNotBlank();
    }

    // ── validate ────────────────────────────────────────────────

    @Test
    @DisplayName("🚫 endDate trước startDate → throw IllegalArgumentException")
    void preview_endDateBeforeStartDate_throwsException() {
        RecurringBookingRequest req = buildRequest(
                "WEEKLY",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 7, 1) // end trước start
        );

        assertThatThrownBy(() -> service.preview(CUSTOMER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày kết thúc phải sau ngày bắt đầu");
    }

    @Test
    @DisplayName("🚫 endTime trước startTime → throw IllegalArgumentException")
    void preview_endTimeBeforeStartTime_throwsException() {
        RecurringBookingRequest req = buildRequest(
                "WEEKLY",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 22)
        );
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(9, 0)); // end trước start

        assertThatThrownBy(() -> service.preview(CUSTOMER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Giờ kết thúc phải sau giờ bắt đầu");
    }

    @Test
    @DisplayName("🚫 Vượt quá MAX_OCCURRENCES (26 lần) → throw IllegalArgumentException")
    void preview_exceedsMaxOccurrences_throwsException() {
        // WEEKLY trong 1 năm = ~52 lần, vượt giới hạn 26
        RecurringBookingRequest req = buildRequest(
                "WEEKLY",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        );

        assertThatThrownBy(() -> service.preview(CUSTOMER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tối đa 26 lần lặp");
    }

    @Test
    @DisplayName("✅ Đúng MAX_OCCURRENCES (26 lần WEEKLY = ~25 tuần) → không bị reject")
    void preview_exactlyAtLimit_doesNotThrow() {
        // 25 tuần * 7 ngày = 175 ngày → 26 occurrences đúng giới hạn
        RecurringBookingRequest req = buildRequest(
                "WEEKLY",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1).plusWeeks(25)
        );

        when(bookingRepository.existsConflict(
                anyLong(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);

        RecurringBookingPreviewResponse response = service.preview(CUSTOMER_ID, req);
        assertThat(response.getTotalOccurrences()).isEqualTo(26);
    }

    @Test
    @DisplayName("🚫 Pattern không hợp lệ → throw IllegalArgumentException")
    void preview_invalidPattern_throwsException() {
        RecurringBookingRequest req = buildRequest(
                "DAILY", // không tồn tại, chỉ có WEEKLY/BIWEEKLY
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 22)
        );

        assertThatThrownBy(() -> service.preview(CUSTOMER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pattern không hợp lệ");
    }
}
