package com.example.salonflow.services;

import com.example.salonflow.dto.review.CreateReviewRequest;
import com.example.salonflow.dto.review.ReviewResponse;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.BusinessAccessDeniedException;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ReviewRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.services.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Booking booking;
    private User customer;
    private Salon salon;
    private Branch branch;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(100L).fullName("Nguyen Van A").build();
        salon = Salon.builder().id(10L).name("Salon Flow Test").build();
        branch = Branch.builder().id(50L).name("Branch Q1").salon(salon).build();

        booking = Booking.builder()
                .id(1L)
                .customer(customer)
                .branch(branch)
                .status(BookingStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("Thành công: Đánh giá booking COMPLETED hợp lệ")
    void testCreateReview_Success() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Dịch vụ tuyệt vời!")
                .photos(List.of("http://example.com/photo1.jpg", "http://example.com/photo2.jpg"))
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(999L);
            return r;
        });
        when(branchRepository.findById(50L)).thenReturn(Optional.of(branch));
        when(salonRepository.findById(10L)).thenReturn(Optional.of(salon));

        ReviewResponse response = reviewService.createReview(1L, request, 100L);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Dịch vụ tuyệt vời!", response.getComment());
        assertEquals(2, response.getPhotos().size());
        assertNotNull(booking.getReviewedAt());

        verify(bookingRepository, times(1)).save(booking);
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(branchRepository, times(1)).save(branch);
        verify(salonRepository, times(1)).save(salon);
    }

    @Test
    @DisplayName("Lỗi 403: Không cho phép đánh giá booking chưa COMPLETED")
    void testCreateReview_StatusNotCompleted_Throws403() {
        booking.setStatus(BookingStatus.PENDING);
        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BusinessAccessDeniedException exception = assertThrows(
                BusinessAccessDeniedException.class,
                () -> reviewService.createReview(1L, request, 100L)
        );

        assertEquals("Chỉ cho phép đánh giá sau khi dịch vụ đã hoàn thành.", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lỗi 403: Khách hàng khác không được đánh giá đơn người khác")
    void testCreateReview_WrongCustomer_Throws403() {
        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BusinessAccessDeniedException exception = assertThrows(
                BusinessAccessDeniedException.class,
                () -> reviewService.createReview(1L, request, 999L) // 999 != 100
        );

        assertEquals("Bạn không có quyền đánh giá đơn đặt lịch này.", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lỗi 403: Không cho phép đánh giá lặp lại đơn đã được review")
    void testCreateReview_AlreadyReviewed_Throws403() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(true);

        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();

        BusinessAccessDeniedException exception = assertThrows(
                BusinessAccessDeniedException.class,
                () -> reviewService.createReview(1L, request, 100L)
        );

        assertEquals("Lịch hẹn này đã được đánh giá trước đó.", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lỗi 400: Không cho phép gửi quá 5 ảnh")
    void testCreateReview_ExceedMaxPhotos_ThrowsException() {
        List<String> sixPhotos = List.of("1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg");
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .photos(sixPhotos)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(1L, request, 100L)
        );

        assertEquals("Tối đa chỉ được tải lên 5 ảnh đánh giá.", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }
}
