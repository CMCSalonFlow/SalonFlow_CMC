package com.example.salonflow.repository;

import com.example.salonflow.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    // Lấy danh sách voucher còn hiệu lực để admin quản lý
    @Query("""
        SELECT v FROM Voucher v
        WHERE v.isActive = true
        ORDER BY v.createdAt DESC
        """)
    List<Voucher> findAllActive();
}
