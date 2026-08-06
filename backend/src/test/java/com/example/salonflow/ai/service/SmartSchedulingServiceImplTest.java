package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.scheduling.*;
import com.example.salonflow.ai.service.impl.SmartSchedulingServiceImpl;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.ShiftStatus;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.pricing.BookingPricingResult;
import com.example.salonflow.pricing.BookingPricingService;
import com.example.salonflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartSchedulingServiceImplTest {

    @Mock
    private SmartSchedulingConfigRepository configRepository;
    @Mock
    private SmartSchedulingLogRepository logRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private BranchHourRepository branchHourRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StaffOffDayRepository staffOffDayRepository;
    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ServiceBundleRepository serviceBundleRepository;
    @Mock
    private BookingPricingService bookingPricingService;

    @InjectMocks
    private SmartSchedulingServiceImpl smartSchedulingService;

    private Branch sampleBranch;
    private BranchHour sampleBranchHour;
    private Staff sampleStaff;
    private SalonService sampleService;
    private SmartSchedulingConfig sampleConfig;

    @BeforeEach
    void setUp() {
        sampleBranch = Branch.builder()
                .id(1L)
                .name("Chi nhánh Quận 1")
                .latitude(10.7769)
                .longitude(106.7009)
                .build();

        sampleBranchHour = BranchHour.builder()
                .id(1L)
                .branch(sampleBranch)
                .dayOfWeek(1)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(20, 0))
                .isClosed(false)
                .build();

        sampleService = SalonService.builder()
                .id(10L)
                .name("Cắt Tóc Nam")
                .durationMinutes(30)
                .build();

        sampleStaff = Staff.builder()
                .id(5L)
                .name("Nguyễn Văn A")
                .specialties("Cắt Tóc Nam, Tạo Kiểu")
                .services(List.of(sampleService))
                .branch(sampleBranch)
                .userId(100L)
                .build();

        sampleConfig = SmartSchedulingConfig.builder()
                .id(1L)
                .branchId(1L)
                .workloadWeight(new BigDecimal("0.400"))
                .travelWeight(new BigDecimal("0.300"))
                .serviceFitWeight(new BigDecimal("0.300"))
                .build();
    }

    @Test
    @DisplayName("Nên đề xuất Top 3 slots khi có dữ liệu rảnh hợp lệ")
    void recommendSlots_ShouldReturnTop3_WhenAvailableSlotsExist() {
        // Arrange
        LocalDate requestDate = LocalDate.of(2026, 8, 10);
        SmartSchedulingRequest request = SmartSchedulingRequest.builder()
                .branchId(1L)
                .date(requestDate)
                .serviceIds(List.of(10L))
                .build();

        BookingPricingResult pricingResult = BookingPricingResult.builder()
                .totalDurationMinutes(30)
                .services(List.of(sampleService))
                .build();

        when(branchRepository.findById(1L)).thenReturn(Optional.of(sampleBranch));
        when(bookingPricingService.calculate(eq(1L), any(), any())).thenReturn(pricingResult);
        when(branchHourRepository.findByBranchIdAndDayOfWeek(eq(1L), anyInt())).thenReturn(Optional.of(sampleBranchHour));
        when(configRepository.findByBranchId(1L)).thenReturn(Optional.of(sampleConfig));
        when(staffRepository.findByBranchId(1L)).thenReturn(List.of(sampleStaff));
        when(staffOffDayRepository.existsByStaffIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(anyLong(), any(), any())).thenReturn(false);
        when(bookingRepository.findByBranchIdAndBookingDateAndStatusIn(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Shift shift = Shift.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .status(ShiftStatus.SCHEDULED)
                .build();
        when(shiftRepository.findByUserIdAndShiftDate(100L, requestDate)).thenReturn(List.of(shift));

        // Act
        SmartSchedulingResponse response = smartSchedulingService.recommendSlots(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getBranchId());
        assertNotNull(response.getRecommendations());
        assertFalse(response.getRecommendations().isEmpty());
        assertTrue(response.getRecommendations().size() <= 3);

        SlotRecommendationDto top1 = response.getRecommendations().get(0);
        assertNotNull(top1.getStartTime());
        assertNotNull(top1.getExplanation());
        assertTrue(top1.getTotalScore() > 0);
    }

    @Test
    @DisplayName("Nên quăng lỗi BadRequestException khi cập nhật tổng trọng số không bằng 1.0")
    void updateConfig_ShouldThrowException_WhenSumIsNotOne() {
        UpdateSmartSchedulingConfigDto invalidDto = UpdateSmartSchedulingConfigDto.builder()
                .workloadWeight(new BigDecimal("0.500"))
                .travelWeight(new BigDecimal("0.300"))
                .serviceFitWeight(new BigDecimal("0.300")) // Sum = 1.1
                .build();

        assertThrows(BadRequestException.class, () -> smartSchedulingService.updateConfig(1L, invalidDto));
    }
}
