package com.example.salonflow.repository;

import com.example.salonflow.entity.Payment;
import com.example.salonflow.entity.enums.PaymentMethod;
import com.example.salonflow.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

/**
 * Kho lưu trữ truy vấn cơ sở dữ liệu cho thực thể Giao dịch thanh toán (Payment).
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);

    Optional<Payment> findFirstByBookingIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
            Long bookingId,
            PaymentMethod paymentMethod,
            PaymentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.idempotencyKey = :idempotencyKey")
    Optional<Payment> findByIdempotencyKeyWithLock(@Param("idempotencyKey") String idempotencyKey);

    @Query("""
            SELECT p.booking.bookingDate AS date, COALESCE(SUM(p.amount), 0) AS revenue
            FROM Payment p
            WHERE p.status = :status
              AND p.booking.branch.id = :branchId
              AND p.booking.bookingDate BETWEEN :startDate AND :endDate
            GROUP BY p.booking.bookingDate
            ORDER BY p.booking.bookingDate
            """)
    List<DailyRevenueProjection> findDailyRevenueByBranch(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") PaymentStatus status
    );
}
