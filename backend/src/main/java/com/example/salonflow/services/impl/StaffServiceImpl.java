package com.example.salonflow.services.impl;

import com.example.salonflow.dto.service.ServiceResponse;
import com.example.salonflow.dto.staff.CreateStaffRequest;
import com.example.salonflow.dto.staff.StaffResponse;
import com.example.salonflow.dto.staff.UpdateStaffRequest;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.Staff;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.repository.StaffRepository;
import com.example.salonflow.services.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

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
    private final SalonRepository salonRepository;
    private final ServiceRepository serviceRepository;

    @Override
    @Transactional
    public StaffResponse create(Long salonId, CreateStaffRequest request) {
        // Kiểm tra sự tồn tại của salon
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy salon với id: " + salonId));

        // Lấy danh sách thực thể dịch vụ từ danh sách ID truyền lên
        List<com.example.salonflow.entity.Service> services = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            services = serviceRepository.findAllById(request.getServiceIds());
        }

        // Xây dựng đối tượng nhân viên mới
        Staff staff = Staff.builder()
                .salon(salon)
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
    public List<StaffResponse> getBySalon(Long salonId) {
        // Kiểm tra xem salon có tồn tại không
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Không tìm thấy salon với id: " + salonId);
        }

        // Tìm danh sách nhân viên và ánh xạ sang DTO trả về
        return staffRepository.findBySalonId(salonId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getById(Long salonId, Long staffId) {
        // Lấy thông tin nhân viên theo id và salonId để đảm bảo nhân viên thuộc salon đó
        Staff staff = staffRepository.findByIdAndSalonId(staffId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên với id: " + staffId + " tại salon: " + salonId));

        return toResponse(staff);
    }

    @Override
    @Transactional
    public StaffResponse update(Long salonId, Long staffId, UpdateStaffRequest request) {
        // Lấy nhân viên cần cập nhật
        Staff staff = staffRepository.findByIdAndSalonId(staffId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên với id: " + staffId + " tại salon: " + salonId));

        // Cập nhật các thông tin cơ bản
        staff.setName(request.getName());
        staff.setAvatarUrl(request.getAvatarUrl());
        staff.setBio(request.getBio());
        staff.setSpecialties(request.getSpecialties());

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
    public void delete(Long salonId, Long staffId) {
        // Lấy nhân viên để xóa
        Staff staff = staffRepository.findByIdAndSalonId(staffId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên với id: " + staffId + " tại salon: " + salonId));

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
                .salonId(staff.getSalon().getId())
                .name(staff.getName())
                .avatarUrl(staff.getAvatarUrl())
                .bio(staff.getBio())
                .specialties(staff.getSpecialties())
                .services(serviceResponses)
                .build();
    }
}
