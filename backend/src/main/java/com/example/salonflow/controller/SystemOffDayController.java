package com.example.salonflow.controller;

import com.example.salonflow.dto.offday.CreateSystemOffDayRequest;
import com.example.salonflow.dto.offday.SystemOffDayResponse;
import com.example.salonflow.services.service.SystemOffDayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system-off-days")
@RequiredArgsConstructor
public class SystemOffDayController {

    private final SystemOffDayService systemOffDayService;

    @GetMapping
    public ResponseEntity<List<SystemOffDayResponse>> getSystemOffDays() {
        return ResponseEntity.ok(systemOffDayService.getSystemOffDays());
    }

    @PostMapping
    public ResponseEntity<SystemOffDayResponse> createSystemOffDay(@Valid @RequestBody CreateSystemOffDayRequest request) {
        return ResponseEntity.ok(systemOffDayService.createSystemOffDay(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSystemOffDay(@PathVariable Long id) {
        systemOffDayService.deleteSystemOffDay(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-branch")
    public ResponseEntity<Boolean> isBranchClosed(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(systemOffDayService.isBranchClosedOnDate(branchId, date));
    }

    @GetMapping("/branch-range")
    public ResponseEntity<List<SystemOffDayResponse>> getOffDaysForBranchAndRange(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(systemOffDayService.getOffDaysForBranchAndRange(branchId, startDate, endDate));
    }
}
