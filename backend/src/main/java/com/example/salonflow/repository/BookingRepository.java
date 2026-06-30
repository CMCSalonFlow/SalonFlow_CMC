package com.example.salonflow.repository;

import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerId(Long customerId);

    List<Booking> findByStaffIdAndBookingDate(Long staffId, LocalDate date);

    List<Booking> findByBranchIdAndBookingDate(Long branchId, LocalDate date);

    Optional<Booking> findBySlotKey(String slotKey);

    List<Booking> findByStatus(BookingStatus status);
}
