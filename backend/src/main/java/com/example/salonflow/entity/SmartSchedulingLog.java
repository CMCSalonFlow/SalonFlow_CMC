package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Thực thể ghi log lại các đề xuất khung giờ thông minh (Smart Scheduling)
 * nhằm phục vụ đánh giá hiệu quả và phân tích trải nghiệm người dùng.
 */
@Entity
@Table(name = "smart_scheduling_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSchedulingLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    // Chuỗi JSON lưu danh sách mã dịch vụ yêu cầu
    @Column(name = "requested_service_ids", columnDefinition = "TEXT")
    private String requestedServiceIds;

    @Column(name = "bundle_id")
    private Long bundleId;

    @Column(name = "preferred_staff_id")
    private Long preferredStaffId;

    // Chuỗi JSON chứa Top 3 gợi ý trả về cho client
    @Column(name = "recommended_slots_json", columnDefinition = "TEXT", nullable = false)
    private String recommendedSlotsJson;

    // Chuỗi JSON chứa thông tin các trọng số đã dùng khi đề xuất
    @Column(name = "weights_used_json", columnDefinition = "TEXT", nullable = false)
    private String weightsUsedJson;

    // Key hoặc mô tả của slot thực tế được khách hàng lựa chọn sau khi gợi ý (nếu có)
    @Column(name = "selected_slot_key")
    private String selectedSlotKey;
}
