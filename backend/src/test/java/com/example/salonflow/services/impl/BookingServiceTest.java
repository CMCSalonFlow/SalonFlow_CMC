package com.example.salonflow.services.impl;

import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.entity.BranchHour;
import com.example.salonflow.repository.BranchHourRepository;
import com.example.salonflow.dto.booking.CreateBookingRequest;
import com.example.salonflow.dto.booking.BookingResponse;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.entity.enums.ShiftStatus;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.pricing.BookingPricingResult;
import com.example.salonflow.pricing.BookingPricingService;
import com.example.salonflow.repository.*;
import com.example.salonflow.websocket.BookingWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử đơn vị cho tầng nghiệp vụ Đặt lịch (BookingServiceImpl) sử dụng
 * Mockito.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

        @Mock
        private BookingRepository bookingRepository;

        @Mock
        private BookingItemRepository bookingItemRepository;

        @Mock
        private BranchRepository branchRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private StaffRepository staffRepository;

        @Mock
        private ServiceRepository serviceRepository;

        @Mock
        private ServiceBundleRepository serviceBundleRepository;

        @Mock
        private BranchHourRepository branchHourRepository;

        @Mock
        private StaffOffDayRepository staffOffDayRepository;

        @Mock
        private ShiftRepository shiftRepository;

        @Mock
        private BookingWebSocketHandler bookingWebSocketHandler;

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        @Mock
        private BookingPricingService bookingPricingService;

        @InjectMocks
        private BookingServiceImpl bookingService;

        private Branch branch;
        private User customer;
        private Staff staff1;
        private Staff staff2;
        private SalonService service1;
        private BranchHour branchHour;
        private Shift scheduledShift;

        @BeforeEach
        void setUp() {
                lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
                lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());
                lenient().when(redisTemplate.delete(any(Collection.class))).thenReturn(0L);
                lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);
                lenient().when(staffOffDayRepository.existsByStaffIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(anyLong(), any(), any()))
                                .thenReturn(false);
                lenient().when(shiftRepository.findByUserIdAndShiftDate(anyLong(), any()))
                                .thenReturn(Collections.emptyList());

                Salon salon = Salon.builder().id(10L).name("Hair Salon").build();
                branch = Branch.builder().id(1L).salon(salon).name("Chi nhánh 1").build();
                customer = User.builder().id(2L).fullName("Nguyễn Khách Hàng").phone("0987654321").build();

                service1 = SalonService.builder()
                                .id(11L)
                                .branch(branch)
                                .name("Cắt tóc nam")
                                .price(BigDecimal.valueOf(80000.00))
                                .durationMinutes(30)
                                .depositRequired(true)
                                .depositPercentage(BigDecimal.valueOf(10))
                                .isActive(true)
                                .images(new ArrayList<>())
                                .build();

                staff1 = Staff.builder()
                                .id(6L)
                                .userId(6L)
                                .branch(branch)
                                .name("Thợ cắt tóc A")
                                .services(new ArrayList<>(List.of(service1)))
                                .build();

                staff2 = Staff.builder()
                                .id(7L)
                                .userId(7L)
                                .branch(branch)
                                .name("Thợ cắt tóc B")
                                .services(new ArrayList<>(List.of(service1)))
                                .build();

                branchHour = BranchHour.builder()
                                .id(100L)
                                .branch(branch)
                                .dayOfWeek(3) // Thứ tư
                                .openTime(LocalTime.of(8, 0))
                                .closeTime(LocalTime.of(20, 0))
                                .isClosed(false)
                                .build();

                scheduledShift = Shift.builder()
                                .id(200L)
                                .branch(branch)
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(20, 0))
                                .shiftDate(LocalDate.of(2026, 7, 1))
                                .status(ShiftStatus.SCHEDULED)
                                .build();
                lenient().when(shiftRepository.findByUserIdAndShiftDate(anyLong(), any()))
                                .thenReturn(List.of(scheduledShift));
        }

        @Test
        @DisplayName("✅ Tạo booking thành công với nhân viên được chỉ định và sẵn sàng")
        void create_withPreferredStaffAvailable_shouldCreateSuccessfully() {
                CreateBookingRequest request = CreateBookingRequest.builder()
                                .customerId(2L)
                                .bookingDate(LocalDate.of(2026, 7, 1)) // 2026-07-01 là Thứ tư
                                .startTime(LocalTime.of(9, 0))
                                .preferredStaffId(6L)
                                .serviceIds(List.of(11L))
                                .notes("Ghi chú hẹn")
                                .build();

                when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
                when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
                BookingPricingResult pricingResult = BookingPricingResult.builder()
                                .totalPrice(BigDecimal.valueOf(80000.00))
                                .depositAmount(BigDecimal.valueOf(8000.00))
                                .remainingAmount(BigDecimal.valueOf(72000.00))
                                .totalDurationMinutes(30)
                                .services(List.of(service1))
                                .build();
                when(bookingPricingService.calculate(eq(1L), eq(List.of(11L)), any())).thenReturn(pricingResult);
                when(branchHourRepository.findByBranchIdAndDayOfWeek(1L, 3)).thenReturn(Optional.of(branchHour));
                when(staffRepository.findByIdAndBranchId(6L, 1L)).thenReturn(Optional.of(staff1));

                // Giả lập không có lịch trùng của nhân sự này
                when(bookingRepository.findOverlappingBookings(any(), any(), any(), any(), any()))
                                .thenReturn(Collections.emptyList());

                when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                        Booking b = invocation.getArgument(0);
                        b.setId(50L);
                        return b;
                });

                when(bookingItemRepository.save(any(BookingItem.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                BookingResponse response = bookingService.create(1L, request);

                assertThat(response.getId()).isEqualTo(50L);
                assertThat(response.getAssignedStaffId()).isEqualTo(6L);
                assertThat(response.getEndTime()).isEqualTo(LocalTime.of(9, 30));
                assertThat(response.getTotalPrice()).isEqualByComparingTo("80000.00");
                assertThat(response.getDepositAmount()).isEqualByComparingTo("8000.00");
        }

        @Test
        @DisplayName("🚫 Tạo booking thất bại khi nhân viên chỉ định bị trùng lịch")
        void create_withPreferredStaffBusy_shouldThrowBusinessException() {
                CreateBookingRequest request = CreateBookingRequest.builder()
                                .customerId(2L)
                                .bookingDate(LocalDate.of(2026, 7, 1))
                                .startTime(LocalTime.of(9, 0))
                                .preferredStaffId(6L)
                                .serviceIds(List.of(11L))
                                .build();

                when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
                when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
                BookingPricingResult pricingResult = BookingPricingResult.builder()
                                .totalPrice(BigDecimal.valueOf(80000.00))
                                .depositAmount(BigDecimal.ZERO)
                                .remainingAmount(BigDecimal.valueOf(80000.00))
                                .totalDurationMinutes(30)
                                .services(List.of(service1))
                                .build();
                when(bookingPricingService.calculate(eq(1L), eq(List.of(11L)), any())).thenReturn(pricingResult);
                when(branchHourRepository.findByBranchIdAndDayOfWeek(1L, 3)).thenReturn(Optional.of(branchHour));
                when(staffRepository.findByIdAndBranchId(6L, 1L)).thenReturn(Optional.of(staff1));

                // Giả lập thợ 6 đã có lịch trùng
                Booking activeBooking = Booking.builder().id(99L).assignedStaff(staff1).build();
                when(bookingRepository.findOverlappingBookings(any(), any(), any(), any(), any()))
                                .thenReturn(List.of(activeBooking));

                assertThatThrownBy(() -> bookingService.create(1L, request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("đã bận vào khung giờ bạn chọn");
        }

        @Test
        @DisplayName("✅ Phân bổ tự động: Chọn nhân viên trống lịch có ít booking nhất trong ngày (Least Bookings)")
        void create_withAnyStaff_shouldAllocateToStaffWithLeastBookings() {
                CreateBookingRequest request = CreateBookingRequest.builder()
                                .customerId(2L)
                                .bookingDate(LocalDate.of(2026, 7, 1))
                                .startTime(LocalTime.of(9, 0))
                                .preferredStaffId(null) // Chọn "Bất kỳ nhân viên"
                                .serviceIds(List.of(11L))
                                .build();

                when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
                when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
                BookingPricingResult pricingResult = BookingPricingResult.builder()
                                .totalPrice(BigDecimal.valueOf(80000.00))
                                .depositAmount(BigDecimal.ZERO)
                                .remainingAmount(BigDecimal.valueOf(80000.00))
                                .totalDurationMinutes(30)
                                .services(List.of(service1))
                                .build();
                when(bookingPricingService.calculate(eq(1L), eq(List.of(11L)), any())).thenReturn(pricingResult);
                when(branchHourRepository.findByBranchIdAndDayOfWeek(1L, 3)).thenReturn(Optional.of(branchHour));

                // Trả về cả thợ A và thợ B
                when(staffRepository.findByBranchId(1L)).thenReturn(List.of(staff1, staff2));

                // Cả 2 đều rảnh giờ đó
                when(bookingRepository.findOverlappingBookings(any(), any(), any(), any(), any()))
                                .thenReturn(Collections.emptyList());

                // Thợ A đã có 2 booking trong ngày, Thợ B chỉ có 1 booking
                Booking bookingMock = Booking.builder().build();
                when(bookingRepository.findByAssignedStaffIdAndBookingDateAndStatusIn(eq(6L), any(), any()))
                                .thenReturn(List.of(bookingMock, bookingMock));
                when(bookingRepository.findByAssignedStaffIdAndBookingDateAndStatusIn(eq(7L), any(), any()))
                                .thenReturn(List.of(bookingMock));

                when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                        Booking b = invocation.getArgument(0);
                        b.setId(51L);
                        return b;
                });

                when(bookingItemRepository.save(any(BookingItem.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                BookingResponse response = bookingService.create(1L, request);

                // Phải phân bổ cho thợ B (ID = 7) do có ít lịch bận hơn trong ngày
                assertThat(response.getAssignedStaffId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("✅ Kiểm tra các khung giờ trống khả dụng thời gian thực")
        void getAvailability_shouldScanCorrectSlots() {
                LocalDate date = LocalDate.of(2026, 7, 1);
                when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
                BookingPricingResult pricingResult = BookingPricingResult.builder()
                                .totalPrice(BigDecimal.valueOf(80000.00))
                                .depositAmount(BigDecimal.ZERO)
                                .remainingAmount(BigDecimal.valueOf(80000.00))
                                .totalDurationMinutes(30)
                                .services(List.of(service1))
                                .build();
                when(bookingPricingService.calculate(eq(1L), eq(List.of(11L)), any())).thenReturn(pricingResult);
                when(staffRepository.findByBranchId(1L)).thenReturn(List.of(staff1));
                when(branchHourRepository.findByBranchIdAndDayOfWeek(1L, 3)).thenReturn(Optional.of(branchHour));
                when(shiftRepository.findByUserIdAndShiftDate(eq(6L), eq(date)))
                                .thenReturn(List.of(scheduledShift));

                // Giả lập thợ A bận từ 09:00 đến 10:00
                Booking bookingA = Booking.builder()
                                .assignedStaff(staff1)
                                .startTime(LocalTime.of(9, 0))
                                .endTime(LocalTime.of(10, 0))
                                .build();
                when(bookingRepository.findByBranchIdAndBookingDateAndStatusIn(eq(1L), eq(date), any()))
                                .thenReturn(List.of(bookingA));

                AvailabilityResponse response = bookingService.getAvailability(1L, date, List.of(11L), null, null);

                // Giờ mở cửa: 08:00 - 20:00. Dịch vụ kéo dài 30 phút.
                // Khoảng 09:00 - 10:00 (gồm các giờ bắt đầu: 09:00, 09:15, 09:30, 09:45) sẽ bị
                // bận.
                assertThat(response.getAvailableStartTimes()).contains(LocalTime.of(8, 0), LocalTime.of(8, 30));
                assertThat(response.getAvailableStartTimes()).doesNotContain(LocalTime.of(9, 0), LocalTime.of(9, 30));
                assertThat(response.getAvailableStartTimes()).contains(LocalTime.of(10, 0), LocalTime.of(10, 30));
        }
}
