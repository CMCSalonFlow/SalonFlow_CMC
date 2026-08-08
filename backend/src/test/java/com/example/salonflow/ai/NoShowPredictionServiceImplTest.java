package com.example.salonflow.ai;

import com.example.salonflow.ai.dto.noshow.NoShowPredictionDto;
import com.example.salonflow.ai.service.impl.NoShowPredictionServiceImpl;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.ZaloZnsService;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoShowPredictionServiceImplTest {

    @Mock
    private NoShowPredictionRepository predictionRepository;

    @Mock
    private NoShowModelConfigRepository configRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ZaloZnsService zaloZnsService;

    @InjectMocks
    private NoShowPredictionServiceImpl noShowPredictionService;

    private Booking mockBooking;
    private User mockCustomer;
    private Branch mockBranch;
    private NoShowModelConfig mockConfig;

    @BeforeEach
    void setUp() {
        mockCustomer = User.builder()
                .id(100L)
                .fullName("Nguyễn Văn A")
                .phone("0987654321")
                .build();

        mockBranch = Branch.builder()
                .id(1L)
                .name("Chi nhánh Quận 1")
                .latitude(10.7769)
                .longitude(106.7009)
                .build();

        mockBooking = Booking.builder()
                .id(999L)
                .customer(mockCustomer)
                .branch(mockBranch)
                .bookingDate(LocalDate.now().plusDays(10)) // Lead time ~240 giờ
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .status(BookingStatus.PENDING)
                .totalPrice(new BigDecimal("200000"))
                .build();

        mockConfig = NoShowModelConfig.builder()
                .id(1L)
                .beta0(new BigDecimal("-1.500"))
                .beta1(new BigDecimal("2.500"))
                .beta2(new BigDecimal("1.200"))
                .beta3(new BigDecimal("1.000"))
                .beta4(new BigDecimal("2.000"))
                .riskThreshold(new BigDecimal("0.700"))
                .autoSendReminder(true)
                .build();
    }

    @Test
    @DisplayName("Kiểm tra dự đoán No-Show nguy cơ cao (HIGH risk) khi khách có lịch sử hủy 100%")
    void testPredictNoShow_HighRisk() {
        // Arrange
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(mockConfig));
        
        // Khách có 2 booking trước đó và đều bị CANCELLED
        Booking past1 = Booking.builder().id(101L).status(BookingStatus.CANCELLED).build();
        Booking past2 = Booking.builder().id(102L).status(BookingStatus.CANCELLED).build();
        when(bookingRepository.findByCustomerId(100L)).thenReturn(List.of(past1, past2));

        when(predictionRepository.findByBookingId(999L)).thenReturn(Optional.empty());
        when(predictionRepository.save(any(NoShowPredictionLog.class))).thenAnswer(i -> {
            NoShowPredictionLog log = i.getArgument(0);
            log.setId(1L);
            return log;
        });
        when(zaloZnsService.sendAppointmentReminderZns(any(), any())).thenReturn(true);

        // Act
        NoShowPredictionDto result = noShowPredictionService.predictAndSaveLog(mockBooking);

        // Assert
        assertNotNull(result);
        assertEquals("HIGH", result.getRiskLevel());
        assertTrue(result.getProbabilityPercentage() >= 70.0);
        assertTrue(result.getIsWarningTriggered());
        assertTrue(result.getSmsSent());
        verify(zaloZnsService, times(1)).sendAppointmentReminderZns(any(), any());
    }

    @Test
    @DisplayName("Kiểm tra dự đoán No-Show uy tín cao (LOW risk) khi khách quen đã hoàn tất nhiều lần")
    void testPredictNoShow_LowRisk() {
        // Arrange
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(mockConfig));

        // Khách đã hoàn tất 10 buổi dịch vụ trước đó
        Booking past1 = Booking.builder().id(201L).status(BookingStatus.COMPLETED).build();
        Booking past2 = Booking.builder().id(202L).status(BookingStatus.COMPLETED).build();
        Booking past3 = Booking.builder().id(203L).status(BookingStatus.COMPLETED).build();
        when(bookingRepository.findByCustomerId(100L)).thenReturn(List.of(past1, past2, past3));

        // Booking ngày mai (lead time ngắn)
        mockBooking.setBookingDate(LocalDate.now().plusDays(1));

        when(predictionRepository.findByBookingId(999L)).thenReturn(Optional.empty());
        when(predictionRepository.save(any(NoShowPredictionLog.class))).thenAnswer(i -> {
            NoShowPredictionLog log = i.getArgument(0);
            log.setId(2L);
            return log;
        });

        // Act
        NoShowPredictionDto result = noShowPredictionService.predictAndSaveLog(mockBooking);

        // Assert
        assertNotNull(result);
        assertNotEquals("HIGH", result.getRiskLevel());
        assertTrue(result.getProbabilityPercentage() < 70.0);
        assertFalse(result.getIsWarningTriggered());
        verify(zaloZnsService, never()).sendAppointmentReminderZns(any(), any());
    }
}
