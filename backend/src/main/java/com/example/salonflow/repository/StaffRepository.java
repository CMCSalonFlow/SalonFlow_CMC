package com.example.salonflow.repository;

import com.example.salonflow.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Kho lưu trữ truy vấn cơ sở dữ liệu cho thực thể Nhân viên (Staff).
 */
public interface StaffRepository extends JpaRepository<Staff, Long> {

    // Tìm tất cả nhân viên thuộc một salon cụ thể
    List<Staff> findBySalonId(Long salonId);

    // Tìm nhân viên theo ID và ID của salon để kiểm soát tính hợp lệ của dữ liệu
    Optional<Staff> findByIdAndSalonId(Long id, Long salonId);
}
