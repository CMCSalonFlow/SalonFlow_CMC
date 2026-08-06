package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Thực thể lưu trữ cấu hình trọng số cho thuật toán Smart Scheduling AI.
 */
@Entity
@Table(name = "smart_scheduling_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSchedulingConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // BranchId có thể null nếu là cấu hình mặc định cho toàn hệ thống
    @Column(name = "branch_id")
    private Long branchId;

    // Trọng số Cân bằng tải công việc nhân viên (Workload Balance)
    @Column(name = "workload_weight", nullable = false, precision = 4, scale = 3)
    @Builder.Default
    private BigDecimal workloadWeight = new BigDecimal("0.400");

    // Trọng số Tối ưu di chuyển / khoảng trống ca làm việc (Travel & Gap Time)
    @Column(name = "travel_weight", nullable = false, precision = 4, scale = 3)
    @Builder.Default
    private BigDecimal travelWeight = new BigDecimal("0.300");

    // Trọng số Mức độ phù hợp kỹ năng / dịch vụ (Service Fit)
    @Column(name = "service_fit_weight", nullable = false, precision = 4, scale = 3)
    @Builder.Default
    private BigDecimal serviceFitWeight = new BigDecimal("0.300");

    // Ghi chú mô tả cấu hình
    @Column(name = "description")
    private String description;
}
