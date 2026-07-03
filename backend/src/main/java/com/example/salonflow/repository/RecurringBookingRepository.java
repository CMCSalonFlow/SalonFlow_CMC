package com.example.salonflow.repository;

import com.example.salonflow.entity.RecurringBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringBookingRepository extends JpaRepository<RecurringBooking, Long> {

    List<RecurringBooking> findByCustomerId(Long customerId);

    List<RecurringBooking> findByStaffId(Long staffId);
}
