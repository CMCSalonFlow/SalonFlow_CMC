package com.example.salonflow.controller;

import com.example.salonflow.dto.Salon.CreateSalonRequest;
import com.example.salonflow.dto.Salon.SalonResponse;
import com.example.salonflow.dto.Salon.UpdateSalonRequest;
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
     * Salon Owner tạo salon
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