package com.example.salonflow.services.impl;

import com.example.salonflow.dto.offday.CreateSystemOffDayRequest;
import com.example.salonflow.dto.offday.SystemOffDayResponse;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.SystemOffDay;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.repository.SystemOffDayRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.SystemOffDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemOffDayServiceImpl implements SystemOffDayService {

    private final SystemOffDayRepository systemOffDayRepository;
    private final SalonRepository salonRepository;
    private final BranchRepository branchRepository;

    @Override
    public List<SystemOffDayResponse> getSystemOffDays() {
        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId).orElse(null);
        Long salonId = salon != null ? salon.getId() : null;

        if (salonId == null) {
            return systemOffDayRepository.findAll().stream().map(this::toResponse).toList();
        }
        return systemOffDayRepository.findBySalonIdOrderByDateFromDesc(salonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SystemOffDayResponse createSystemOffDay(CreateSystemOffDayRequest request) {
        if (request.getDateTo().isBefore(request.getDateFrom())) {
            throw new IllegalArgumentException("Đến ngày không được nhỏ hơn Từ ngày!");
        }

        Long ownerId = SecurityUtils.getCurrentUserId();
        Salon salon = salonRepository.findFirstByOwnerId(ownerId).orElse(null);

        Branch branch = null;
        boolean isAllBranches = Boolean.TRUE.equals(request.getIsAllBranches());
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh id: " + request.getBranchId()));
            isAllBranches = false;
        } else {
            isAllBranches = true;
        }

        SystemOffDay offDay = SystemOffDay.builder()
                .title(request.getTitle().trim())
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .salon(salon)
                .branch(branch)
                .isAllBranches(isAllBranches)
                .reason(request.getReason())
                .build();

        return toResponse(systemOffDayRepository.save(offDay));
    }

    @Override
    @Transactional
    public void deleteSystemOffDay(Long id) {
        SystemOffDay offDay = systemOffDayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngày nghỉ id: " + id));
        systemOffDayRepository.delete(offDay);
    }

    @Override
    public boolean isBranchClosedOnDate(Long branchId, LocalDate date) {
        if (branchId == null || date == null) return false;
        Branch branch = branchRepository.findById(branchId).orElse(null);
        Long salonId = branch != null && branch.getSalon() != null ? branch.getSalon().getId() : null;

        List<SystemOffDay> list = systemOffDayRepository.findOffDaysForBranchAndRange(salonId, branchId, date, date);
        return !list.isEmpty();
    }

    @Override
    public List<SystemOffDayResponse> getOffDaysForBranchAndRange(Long branchId, LocalDate startDate, LocalDate endDate) {
        if (branchId == null || startDate == null || endDate == null) return List.of();
        Branch branch = branchRepository.findById(branchId).orElse(null);
        Long salonId = branch != null && branch.getSalon() != null ? branch.getSalon().getId() : null;

        return systemOffDayRepository.findOffDaysForBranchAndRange(salonId, branchId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SystemOffDayResponse toResponse(SystemOffDay entity) {
        long days = ChronoUnit.DAYS.between(entity.getDateFrom(), entity.getDateTo()) + 1;
        return SystemOffDayResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .dateFrom(entity.getDateFrom())
                .dateTo(entity.getDateTo())
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
                .branchName(entity.getBranch() != null ? entity.getBranch().getName() : "Toàn bộ Salon")
                .isAllBranches(entity.getIsAllBranches())
                .reason(entity.getReason())
                .totalDays(days)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
