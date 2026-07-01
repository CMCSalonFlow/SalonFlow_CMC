package com.example.salonflow.services.impl;

import com.example.salonflow.dto.service.ServiceResponse;
import com.example.salonflow.dto.staff.CreateStaffRequest;
import com.example.salonflow.dto.staff.StaffResponse;
import com.example.salonflow.dto.staff.UpdateStaffRequest;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Staff;
import com.example.salonflow.exception.ResourceNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai các nghiệp vụ quản lý nhân viên (StaffService).
 * Sử dụng tên đầy đủ cho chú thích @Service của Spring để tránh xung đột với thực thể Service của dự án.
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

    @Override
    @Transactional
    public StaffResponse create(Long branchId, CreateStaffRequest request) {
        // Kiểm tra sự tồn tại của chi nhánh (Branch)
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh với id: " + branchId));

        // Lấy danh sách thực thể dịch vụ từ danh sách ID truyền lên
        List<com.example.salonflow.entity.Service> services = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            services = serviceRepository.findAllById(request.getServiceIds());
        }

        UserBranch userBranch = null;
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng với id: " + request.getUserId()));

            // Gán người dùng vào chi nhánh (user_branches) nếu chưa tồn tại
            UserBranchId userBranchId = new UserBranchId(user.getId(), branch.getId());
            userBranch = userBranchRepository.findById(userBranchId).orElse(null);

            if (userBranch == null) {
                userBranch = UserBranch.builder()
                        .id(userBranchId)
                        .user(user)
                        .branch(branch)
                        .assignedAt(Instant.now())
                        .build();
                userBranch = userBranchRepository.save(userBranch);
            }

            // Gán role STAFF nếu người dùng chưa có
            Role staffRole = roleRepository.findByCode("STAFF")
                    .orElseThrow(() -> new ResourceNotFoundException("Role STAFF not found"));
            boolean hasStaffRole = user.getUserRoles().stream()
                    .anyMatch(ur -> ur.getRole().getCode().equals("STAFF"));
            if (!hasStaffRole) {
                UserRole userRole = UserRole.builder()
                        .id(new UserRoleId(user.getId(), staffRole.getId()))
                        .user(user)
                        .role(staffRole)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userRoleRepository.save(userRole);
            }
        }

        // Xây dựng đối tượng nhân viên mới liên kết với chi nhánh
        Staff staff = Staff.builder()
                .branch(branch)
                .userId(request.getUserId())
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

        // Tìm danh sách nhân viên của chi nhánh và ánh xạ sang DTO trả về
        return staffRepository.findByBranchId(branchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getById(Long branchId, Long staffId) {
        // Lấy thông tin nhân viên theo id và branchId để đảm bảo nhân viên thuộc chi nhánh đó
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

        // Cập nhật liên kết tài khoản người dùng
        UserBranch userBranch = null;
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng với id: " + request.getUserId()));

            // Gán người dùng vào chi nhánh (user_branches) nếu chưa tồn tại
            UserBranchId userBranchId = new UserBranchId(user.getId(), branchId);
            userBranch = userBranchRepository.findById(userBranchId).orElse(null);

            if (userBranch == null) {
                Branch branch = staff.getBranch();
                userBranch = UserBranch.builder()
                        .id(userBranchId)
                        .user(user)
                        .branch(branch)
                        .assignedAt(Instant.now())
                        .build();
                userBranch = userBranchRepository.save(userBranch);
            }

            // Gán role STAFF nếu người dùng chưa có
            Role staffRole = roleRepository.findByCode("STAFF")
                    .orElseThrow(() -> new ResourceNotFoundException("Role STAFF not found"));
            boolean hasStaffRole = user.getUserRoles().stream()
                    .anyMatch(ur -> ur.getRole().getCode().equals("STAFF"));
            if (!hasStaffRole) {
                UserRole userRole = UserRole.builder()
                        .id(new UserRoleId(user.getId(), staffRole.getId()))
                        .user(user)
                        .role(staffRole)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userRoleRepository.save(userRole);
            }
        }
        staff.setUserBranch(userBranch);
        staff.setUserId(request.getUserId());

        // Cập nhật lại liên kết danh sách dịch vụ cho phép thực hiện
        List<com.example.salonflow.entity.Service> services = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            services = serviceRepository.findAllById(request.getServiceIds());
        }
        staff.setServices(services);

        staff = staffRepository.save(staff);
        return toResponse(staff);
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
            for (com.example.salonflow.entity.Service service : staff.getServices()) {
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

        return StaffResponse.builder()
                .id(staff.getId())
                .branchId(staff.getBranch().getId())
                .name(staff.getName())
                .avatarUrl(staff.getAvatarUrl())
                .bio(staff.getBio())
                .specialties(staff.getSpecialties())
                .services(serviceResponses)
                .userId(staff.getUserBranch() != null ? staff.getUserBranch().getId().getUserId() : null)
                .build();
    }
}
