package com.example.salonflow.services.impl;

import com.example.salonflow.dto.service.ServiceResponse;
import com.example.salonflow.dto.staff.CreateStaffRequest;
import com.example.salonflow.dto.staff.PublicStaffResponse;
import com.example.salonflow.dto.staff.StaffResponse;
import com.example.salonflow.dto.staff.UpdateStaffRequest;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Staff;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.entity.enums.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.repository.StaffRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.repository.UserBranchRepository;
import com.example.salonflow.repository.RoleRepository;
import com.example.salonflow.repository.UserRoleRepository;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.UserBranch;
import com.example.salonflow.entity.UserBranchId;
import com.example.salonflow.entity.Role;
import com.example.salonflow.entity.UserRole;
import com.example.salonflow.entity.UserRoleId;
import com.example.salonflow.services.service.StaffService;
import com.example.salonflow.services.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai các nghiệp vụ quản lý nhân viên (StaffService).
 * Sử dụng tên đầy đủ cho chú thích @Service của Spring để tránh xung đột với
 * thực thể Service của dự án.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final BranchRepository branchRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final UserBranchRepository userBranchRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public StaffResponse create(Long branchId, CreateStaffRequest request) {
        // Kiểm tra sự tồn tại của chi nhánh (Branch)
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId));

        // Kiểm tra giới hạn nhân viên
        subscriptionService.validateStaffLimit(branch.getSalon().getId());

        // Kiểm tra xem email đã tồn tại trong bảng users chưa
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email đăng nhập này đã được sử dụng bởi một tài khoản khác trong hệ thống.");
        }

        // Lấy danh sách thực thể dịch vụ từ danh sách ID truyền lên
        List<com.example.salonflow.entity.SalonService> services = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            services = serviceRepository.findAllById(request.getServiceIds());
        }

        // Tạo mới User với mật khẩu mặc định "Staff@123"
        String defaultPassword = "Staff@123";
        User user = User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(defaultPassword))
                .fullName(request.getName())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .mustChangePassword(true)
                .build();
        user = userRepository.save(user);

        // Gán role (STAFF hoặc MANAGER)
        String roleToAssign = ("MANAGER".equalsIgnoreCase(request.getRoleCode())) ? "MANAGER" : "STAFF";
        Role staffRole = roleRepository.findByCode(roleToAssign)
                .orElseGet(() -> roleRepository.findByCode("STAFF")
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found")));
        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(user.getId(), staffRole.getId()))
                .user(user)
                .role(staffRole)
                .assignedAt(LocalDateTime.now())
                .build();
        userRoleRepository.save(userRole);

        // Gán vào chi nhánh (user_branches)
        UserBranchId userBranchId = new UserBranchId(user.getId(), branch.getId());
        UserBranch userBranch = UserBranch.builder()
                .id(userBranchId)
                .user(user)
                .branch(branch)
                .assignedAt(Instant.now())
                .build();
        userBranch = userBranchRepository.save(userBranch);

        // Xây dựng đối tượng nhân viên mới liên kết với chi nhánh
        Staff staff = Staff.builder()
                .branch(branch)
                .userId(user.getId())
                .userBranch(userBranch)
                .name(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .bio(request.getBio())
                .specialties(request.getSpecialties())
                .services(services)
                .build();

        staff = staffRepository.save(staff);
        return toResponse(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> getByBranch(Long branchId) {
        // Kiểm tra xem chi nhánh có tồn tại không
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId);
        }

        // Tìm danh sách nhân viên đang hoạt động của chi nhánh và ánh xạ sang DTO trả
        // về
        return staffRepository.findByBranchId(branchId).stream()
                .filter(staff -> {
                    if (staff.getUserId() == null)
                        return true;
                    User user = userRepository.findById(staff.getUserId()).orElse(null);
                    return user != null && user.getStatus() == UserStatus.ACTIVE;
                })
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicStaffResponse> getPublicByBranch(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId);
        }

        return staffRepository.findByBranchId(branchId).stream()
                .filter(staff -> {
                    if (staff.getUserId() == null)
                        return true;
                    User user = userRepository.findById(staff.getUserId()).orElse(null);
                    return user != null && user.getStatus() == UserStatus.ACTIVE;
                })
                .map(this::toPublicResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getById(Long branchId, Long staffId) {
        // Lấy thông tin nhân viên theo id và branchId để đảm bảo nhân viên thuộc chi
        // nhánh đó
        Staff staff = staffRepository.findByIdAndBranchId(staffId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên với id: " + staffId + " tại chi nhánh: " + branchId));

        return toResponse(staff);
    }

    @Override
    @Transactional
    public StaffResponse update(Long branchId, Long staffId, UpdateStaffRequest request) {
        // Lấy nhân viên cần cập nhật
        Staff staff = staffRepository.findByIdAndBranchId(staffId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên với id: " + staffId + " tại chi nhánh: " + branchId));

        // Cập nhật các thông tin cơ bản
        staff.setName(request.getName());
        staff.setAvatarUrl(request.getAvatarUrl());
        staff.setBio(request.getBio());
        staff.setSpecialties(request.getSpecialties());

        // Cập nhật tên và role của User liên kết để đồng bộ
        if (staff.getUserId() != null) {
            User user = userRepository.findById(staff.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy tài khoản người dùng liên kết với id: " + staff.getUserId()));
            user.setFullName(request.getName());
            userRepository.save(user);

            if (request.getRoleCode() != null && !request.getRoleCode().isBlank()) {
                String targetRoleCode = "MANAGER".equalsIgnoreCase(request.getRoleCode()) ? "MANAGER" : "STAFF";
                Role newRole = roleRepository.findByCode(targetRoleCode).orElse(null);
                if (newRole != null) {
                    userRoleRepository.deleteByUser_Id(user.getId());
                    UserRole userRole = UserRole.builder()
                            .id(new UserRoleId(user.getId(), newRole.getId()))
                            .user(user)
                            .role(newRole)
                            .assignedAt(LocalDateTime.now())
                            .build();
                    userRoleRepository.save(userRole);
                }
            }
        }

        // Cập nhật lại liên kết danh sách dịch vụ cho phép thực hiện
        List<com.example.salonflow.entity.SalonService> services = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            services = serviceRepository.findAllById(request.getServiceIds());
        }
        staff.setServices(services);

        Staff savedStaff = staffRepository.save(staff);
        return toResponse(savedStaff);
    }

    @Override
    @Transactional
    public void delete(Long branchId, Long staffId) {
        // Lấy nhân viên để xóa
        Staff staff = staffRepository.findByIdAndBranchId(staffId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên với id: " + staffId + " tại chi nhánh: " + branchId));

        staffRepository.delete(staff);
    }

    /**
     * Chuyển đổi từ Thực thể Staff sang DTO phản hồi trả về FE.
     */
    private StaffResponse toResponse(Staff staff) {
        List<ServiceResponse> serviceResponses = new ArrayList<>();
        if (staff.getServices() != null) {
            for (com.example.salonflow.entity.SalonService service : staff.getServices()) {
                // Sắp xếp thứ tự ảnh hiển thị của dịch vụ
                List<String> imageUrls = service.getImages().stream()
                        .sorted((a, b) -> {
                            Integer oa = a.getDisplayOrder() == null ? 0 : a.getDisplayOrder();
                            Integer ob = b.getDisplayOrder() == null ? 0 : b.getDisplayOrder();
                            return oa.compareTo(ob);
                        })
                        .map(com.example.salonflow.entity.ServiceImage::getImageUrl)
                        .toList();

                serviceResponses.add(ServiceResponse.builder()
                        .id(service.getId())
                        .branchId(service.getBranch().getId())
                        .categoryId(service.getCategory() != null ? service.getCategory().getId() : null)
                        .categoryName(service.getCategory() != null ? service.getCategory().getName() : null)
                        .name(service.getName())
                        .price(service.getPrice())
                        .durationMinutes(service.getDurationMinutes())
                        .description(service.getDescription())
                        .isActive(service.getIsActive())
                        .images(imageUrls)
                        .build());
            }
        }

        String email = null;
        String phone = null;
        String roleCode = "STAFF";
        if (staff.getUserId() != null) {
            User user = userRepository.findById(staff.getUserId()).orElse(null);
            if (user != null) {
                email = user.getEmail();
                phone = user.getPhone();
                if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
                    boolean isManager = user.getUserRoles().stream()
                            .anyMatch(ur -> ur.getRole() != null && "MANAGER".equalsIgnoreCase(ur.getRole().getCode()));
                    if (isManager) {
                        roleCode = "MANAGER";
                    }
                }
            }
        }

        return StaffResponse.builder()
                .id(staff.getId())
                .branchId(staff.getBranch().getId())
                .name(staff.getName())
                .avatarUrl(staff.getAvatarUrl())
                .bio(staff.getBio())
                .specialties(staff.getSpecialties())
                .services(serviceResponses)
                .userId(staff.getUserId())
                .email(email)
                .phone(phone)
                .roleCode(roleCode)
                .build();
    }

    private PublicStaffResponse toPublicResponse(Staff staff) {
        List<ServiceResponse> serviceResponses = new ArrayList<>();
        if (staff.getServices() != null) {
            for (com.example.salonflow.entity.SalonService service : staff.getServices()) {
                serviceResponses.add(ServiceResponse.builder()
                        .id(service.getId())
                        .branchId(service.getBranch().getId())
                        .categoryId(service.getCategory() != null ? service.getCategory().getId() : null)
                        .categoryName(service.getCategory() != null ? service.getCategory().getName() : null)
                        .name(service.getName())
                        .price(service.getPrice())
                        .durationMinutes(service.getDurationMinutes())
                        .description(service.getDescription())
                        .isActive(service.getIsActive())
                        .build());
            }
        }

        PublicStaffResponse response = new PublicStaffResponse();
        response.setId(staff.getId());
        response.setBranchId(staff.getBranch().getId());
        response.setName(staff.getName());
        response.setAvatarUrl(staff.getAvatarUrl());
        response.setBio(staff.getBio());
        response.setSpecialties(staff.getSpecialties());
        response.setServices(serviceResponses);
        response.setUserId(staff.getUserId());
        return response;
    }
}
