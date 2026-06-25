package com.example.salonflow.controller;

import com.example.salonflow.dto.offday.StaffOffDayRequest;
import com.example.salonflow.dto.offday.StaffOffDayResponse;
import com.example.salonflow.services.service.StaffOffDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffOffDayController {

    private final StaffOffDayService staffOffDayService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    @PatchMapping("/{staffId}/off-days")
    public ResponseEntity<StaffOffDayResponse> createOffDay(
            @PathVariable Long staffId,
            @RequestBody @Valid StaffOffDayRequest request) {
        
        StaffOffDayResponse response = staffOffDayService.createOffDay(staffId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    @GetMapping("/{staffId}/off-days")
    public ResponseEntity<List<StaffOffDayResponse>> getOffDays(@PathVariable Long staffId) {
        List<StaffOffDayResponse> offDays = staffOffDayService.getOffDaysByStaffId(staffId);
        return ResponseEntity.ok(offDays);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    @PutMapping("/off-days/{offDayId}")
    public ResponseEntity<StaffOffDayResponse> updateOffDay(
            @PathVariable Long offDayId,
            @RequestBody @Valid StaffOffDayRequest request) {
        
        StaffOffDayResponse response = staffOffDayService.updateOffDay(offDayId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OWNER')")
    @DeleteMapping("/off-days/{offDayId}")
    public ResponseEntity<Void> deleteOffDay(@PathVariable Long offDayId) {
        staffOffDayService.deleteOffDay(offDayId);
        return ResponseEntity.noContent().build();
    }

    // API public hơn một chút (có thể staff tự xem lịch nghỉ của mình sau)
    @GetMapping("/{staffId}/off-check")
    public ResponseEntity<Boolean> checkOffDay(
            @PathVariable Long staffId,
            @RequestParam LocalDate date) {
        
        boolean isOff = staffOffDayService.isStaffOffInPeriod(staffId, date);
        return ResponseEntity.ok(isOff);
    }
}