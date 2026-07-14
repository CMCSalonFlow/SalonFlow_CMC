package com.example.salonflow.repository;

import com.example.salonflow.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    boolean existsByVoucherIdAndUserId(Long voucherId, Long userId);
}
