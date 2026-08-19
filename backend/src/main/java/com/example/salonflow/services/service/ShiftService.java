package com.example.salonflow.services.service;

import com.example.salonflow.dto.shift.*;

import java.time.LocalDate;
import java.util.List;

public interface ShiftService {

    // ── Template CRUD ─────────────────────────────────────────
    ShiftTemplateResponse createTemplate(CreateShiftTemplateRequest request);

    List<ShiftTemplateResponse> getTemplatesByUser(Long userId, Long branchId);

    ShiftTemplateResponse getTemplateById(Long templateId);

    ShiftTemplateResponse updateTemplate(Long templateId, CreateShiftTemplateRequest request);

    void deleteTemplate(Long templateId);

    // ── Áp dụng template vào tuần ─────────────────────────────
    List<ShiftResponse> applyTemplate(Long templateId, ApplyTemplateRequest request);

    List<ShiftResponse> applyAllTemplatesForBranch(Long branchId, ApplyTemplateRequest request);

    // ── Query shifts ──────────────────────────────────────────
    List<ShiftResponse> getShiftsByUserAndWeek(Long userId, LocalDate weekStartDate);

    List<ShiftResponse> getShiftsByBranchAndDate(Long branchId, LocalDate date);

    List<ShiftResponse> getShiftsByBranchAndRange(Long branchId, LocalDate startDate, LocalDate endDate);

    // ── Availability slots cho booking ────────────────────────
    List<AvailabilitySlotResponse> getAvailabilitySlots(Long branchId, LocalDate date);
}
