package com.example.salonflow.repository;

import com.example.salonflow.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Kho lưu trữ truy vấn cơ sở dữ liệu cho thực thể Nhân viên (Staff).
 */
public interface StaffRepository extends JpaRepository<Staff, Long> {

    // Tìm tất cả nhân viên thuộc một chi nhánh (Branch) cụ thể
    List<Staff> findByBranchId(Long branchId);

    // Tìm nhân viên theo ID và ID chi nhánh để kiểm soát tính hợp lệ của dữ liệu
    Optional<Staff> findByIdAndBranchId(Long id, Long branchId);

    // Tìm tất cả nhân viên thuộc một Salon
    List<Staff> findByBranchSalonId(Long salonId);
}
