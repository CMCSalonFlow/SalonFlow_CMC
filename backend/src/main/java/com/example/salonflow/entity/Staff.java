package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể đại diện cho nhân viên (Staff) của chi nhánh (Branch).
 */
@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với Chi nhánh (Branch) sở hữu nhân viên này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // ID tài khoản người dùng liên kết
    @Column(name = "user_id")
    private Long userId;

    // Liên kết tài khoản người dùng tại chi nhánh (UserBranch) của nhân viên (read-only)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false),
        @JoinColumn(name = "branch_id", referencedColumnName = "branch_id", insertable = false, updatable = false)
    })
    private UserBranch userBranch;

    // Tên của nhân viên
    @Column(nullable = false)
    private String name;

    // Đường dẫn ảnh đại diện
    @Column(name = "avatar_url")
    private String avatarUrl;

    // Mô tả tiểu sử, giới thiệu bản thân
    @Column(columnDefinition = "TEXT")
    private String bio;

    // Danh sách chuyên môn hoặc tag kỹ năng (phân tách bởi dấu phẩy)
    @Column(name = "specialties")
    private String specialties;

    // Danh sách các dịch vụ nhân viên này được phép thực hiện (nhiều-nhiều)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "staff_services",
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private List<Service> services = new ArrayList<>();
}
