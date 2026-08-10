package com.example.salonflow;

import com.example.salonflow.dto.booking.CreateBookingRequest;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "app.notification.email.enabled=false",
    "resend.api-key=mock-api-key",
    "resend.from=mock-from",
    "spring.security.oauth2.client.registration.google.client-id=mock-id",
    "spring.security.oauth2.client.registration.google.client-secret=mock-secret"
})
public class ConcurrentBookingTest {

    @Autowired
    private BookingService bookingService;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private BookingItemRepository bookingItemRepository;

    @MockBean
    private BranchRepository branchRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private StaffRepository staffRepository;

    @MockBean
    private ServiceRepository serviceRepository;

    @MockBean
    private ServiceBundleRepository serviceBundleRepository;

    @MockBean
    private BranchHourRepository branchHourRepository;

    @MockBean
    private com.example.salonflow.ai.service.NoShowPredictionService noShowPredictionService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testConcurrentBookings() throws InterruptedException, ExecutionException {
        // 1. Prepare entities
        Branch branch = Branch.builder().name("Test Branch").build();
        branch.setId(1L);

        User customer = User.builder().fullName("Test Customer").email("customer@test.com").build();
        customer.setId(2L);

        SalonService service = SalonService.builder().name("Cắt Tóc").price(BigDecimal.valueOf(100000)).durationMinutes(30).branch(branch).build();
        service.setId(3L);

        Staff staff = Staff.builder().name("Thợ A").userId(4L).branch(branch).services(List.of(service)).build();
        staff.setId(5L);

        BranchHour branchHour = BranchHour.builder().branch(branch).dayOfWeek(1).openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(21, 0)).isClosed(false).build();

        // 2. Setup mock answers
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(serviceRepository.findAllById(List.of(3L))).thenReturn(List.of(service));
        when(staffRepository.findByIdAndBranchId(5L, 1L)).thenReturn(Optional.of(staff));
        when(branchHourRepository.findByBranchIdAndDayOfWeek(eq(1L), anyInt())).thenReturn(Optional.of(branchHour));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), anyList()))
                .thenReturn(new ArrayList<>());
        
        // Mock save returning booking
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(999L);
            return b;
        });

        // Mock save returning booking item
        when(bookingItemRepository.save(any(BookingItem.class))).thenAnswer(invocation -> {
            BookingItem item = invocation.getArgument(0);
            item.setId(777L);
            return item;
        });

        // 3. Clear existing redis key to avoid noise
        String lockKey = String.format("lock:booking:%d:%s:%s", 5L, "2026-07-13", "10:00:00");
        redisTemplate.delete(lockKey);

        // 4. Create request
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerId(2L);
        request.setServiceIds(List.of(3L));
        request.setBookingDate(LocalDate.of(2026, 7, 13)); // 2026-07-13 is a Monday (DayOfWeek 1)
        request.setStartTime(LocalTime.of(10, 0));
        request.setPreferredStaffId(5L);

        // 5. Trigger concurrent execution using CountDownLatch
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errorMessages = new java.util.concurrent.CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to align
                    bookingService.create(1L, request);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                    errorMessages.add(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Release the latch to start concurrent execution
        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // 6. Assertions
        System.out.println(">>> SUCCESS COUNT: " + successCount.get());
        System.out.println(">>> FAILURE COUNT: " + failureCount.get());
        System.out.println(">>> ERROR MESSAGES: " + errorMessages);

        // Verify that exactly ONE thread succeeded, and all other threads failed
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
        assertThat(errorMessages).allMatch(msg -> msg.contains("song song"));

        // Clean up
        redisTemplate.delete(lockKey);
    }
}
