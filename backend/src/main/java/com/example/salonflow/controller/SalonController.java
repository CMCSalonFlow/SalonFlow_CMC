package com.example.salonflow.controller;

import com.example.salonflow.dto.Salon.*;
import com.example.salonflow.entity.SalonStatus;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.SalonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salons")
@RequiredArgsConstructor
public class SalonController {

    private final SalonService salonService;

    /**
     * Salon Owner tạo salon mới (Trạng thái mặc định: PENDING)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SALON_OWNER')")
    public SalonResponse createSalon(
            @Valid @RequestBody CreateSalonRequest request
    ) {
        return salonService.create(request);
    }

    /**
     * Salon Owner xem salon của mình
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('SALON_OWNER')")
    public SalonResponse getMySalon() {
        return salonService.getMine();
    }

    /**
     * Salon Owner cập nhật salon của mình
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('SALON_OWNER')")
    public SalonResponse updateSalon(
            @Valid @RequestBody UpdateSalonRequest request
    ) {
        return salonService.update(request);
    }

    /**
     * Salon Owner gửi lại đơn đăng ký (Appeal) sau 7 ngày bị từ chối
     */
    @PostMapping("/me/appeal")
    @PreAuthorize("hasRole('SALON_OWNER')")
    public SalonResponse appealSalon() {
        SalonResponse mySalon = salonService.getMine();
        return salonService.appeal(mySalon.getId());
    }

    /**
     * Salon Owner xóa salon của mình
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SALON_OWNER')")
    public void deleteSalon() {
        salonService.delete();
    }

    /**
     * Super Admin xem danh sách tất cả salon
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SalonResponse> getAllSalons() {
        return salonService.getAll();
    }

    /**
     * Super Admin lọc danh sách salon theo trạng thái (PENDING, APPROVED, REJECTED, SUSPENDED)
     */
    @GetMapping("/admin/by-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SalonResponse> getSalonsByStatus(
            @RequestParam("status") SalonStatus status
    ) {
        return salonService.getByStatus(status);
    }

    /**
     * Super Admin duyệt đơn đăng ký Salon
     */
    @PostMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SalonResponse approveSalon(
            @PathVariable Long id
    ) {
        Long adminUserId = SecurityUtils.getCurrentUserId();
        return salonService.approve(id, adminUserId);
    }

    /**
     * Super Admin từ chối đơn đăng ký Salon kèm lý do
     */
    @PostMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SalonResponse rejectSalon(
            @PathVariable Long id,
            @Valid @RequestBody RejectSalonRequest request
    ) {
        Long adminUserId = SecurityUtils.getCurrentUserId();
        return salonService.reject(id, request, adminUserId);
    }

    /**
     * Super Admin xem lịch sử Audit của Salon
     */
    @GetMapping("/admin/audits/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SalonApprovalAuditResponse> getSalonAudits(
            @PathVariable Long id
    ) {
        return salonService.getAudits(id);
    }

    /**
     * Khách hàng xem danh sách tất cả salon công khai (chỉ những salon đã APPROVED)
     */
    @GetMapping("/public")
    public List<SalonResponse> getPublicSalons() {
        return salonService.getPublicSalons();
    }

    /**
     * Khách hàng / Khách vãng lai tìm các salon và chi nhánh gần nhất bằng tọa độ GPS (PostGIS ST_Distance)
     */
    @GetMapping("/nearby")
    public List<com.example.salonflow.dto.Salon.NearbySalonBranchResponse> getNearbySalons(
            @RequestParam(name = "lat") Double latitude,
            @RequestParam(name = "lng") Double longitude,
            @RequestParam(name = "radius", required = false, defaultValue = "5000") Double radius,
            @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit
    ) {
        return salonService.getNearbySalons(latitude, longitude, radius, limit);
    }

    /**
     * Super Admin xem chi tiết salon
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SalonResponse getSalonById(
            @PathVariable Long id
    ) {
        return salonService.getById(id);
    }

}