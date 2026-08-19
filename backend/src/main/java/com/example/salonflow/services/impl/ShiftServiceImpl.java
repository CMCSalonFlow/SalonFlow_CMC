package com.example.salonflow.services.impl;

import com.example.salonflow.dto.shift.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.ShiftStatus;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftTemplateRepository templateRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    // ── Template CRUD ──────────────────────────────────────────

    @Override
    @Transactional
    public ShiftTemplateResponse createTemplate(CreateShiftTemplateRequest request) {

        User user = findUser(request.getUserId());
        Branch branch = findBranch(request.getBranchId());

        ShiftTemplate template = ShiftTemplate.builder()
                .user(user)
                .branch(branch)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        if (request.getDetails() != null) {
            validateDetailsNoOverlap(request.getDetails());
            List<ShiftTemplateDetail> details = buildDetails(request.getDetails(), template);
            template.getDetails().addAll(details);
        }

        template = templateRepository.save(template);
        return toTemplateResponse(template);
    }

    @Override
    public List<ShiftTemplateResponse> getTemplatesByUser(Long userId, Long branchId) {
        if (userId == null) {
            return templateRepository.findByBranchId(branchId)
                    .stream()
                    .map(this::toTemplateResponse)
                    .toList();
        }
        return templateRepository.findByUserIdAndBranchId(userId, branchId)
                .stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Override
    public ShiftTemplateResponse getTemplateById(Long templateId) {
        ShiftTemplate template = findTemplate(templateId);
        return toTemplateResponse(template);
    }

    @Override
    @Transactional
    public ShiftTemplateResponse updateTemplate(
            Long templateId,
            CreateShiftTemplateRequest request
    ) {
        ShiftTemplate template = findTemplate(templateId);

        template.setName(request.getName());
        template.setDescription(request.getDescription());

        if (request.getDetails() != null) {
            validateDetailsNoOverlap(request.getDetails());
            template.getDetails().clear();
            List<ShiftTemplateDetail> details = buildDetails(request.getDetails(), template);
            template.getDetails().addAll(details);
        }

        template = templateRepository.save(template);
        return toTemplateResponse(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        ShiftTemplate template = findTemplate(templateId);
        templateRepository.delete(template);
    }

    // ── Áp dụng template vào tuần ──────────────────────────────

    @Override
    @Transactional
    public List<ShiftResponse> applyTemplate(
            Long templateId,
            ApplyTemplateRequest request
    ) {
        ShiftTemplate template = findTemplate(templateId);

        // Đảm bảo weekStartDate là Thứ 2
        LocalDate monday = request.getWeekStartDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Shift> createdShifts = new ArrayList<>();

        for (ShiftTemplateDetail detail : template.getDetails()) {
            // dayOfWeek: 1=Thứ 2 → offset 0, 2=Thứ 3 → offset 1...
            LocalDate shiftDate = monday.plusDays(detail.getDayOfWeek() - 1);

            // Kiểm tra overlap
            boolean hasOverlap = shiftRepository.existsOverlappingShift(
                    template.getUser().getId(),
                    template.getBranch().getId(),
                    shiftDate,
                    detail.getStartTime(),
                    detail.getEndTime(),
                    null
            );

            if (hasOverlap) {
                if (!request.isOverwrite()) {
                    // Bỏ qua ngày đã có shift (không ghi đè)
                    continue;
                }
                // Xóa shift cũ bị overlap trước khi tạo mới
                List<Shift> existingShifts = shiftRepository
                        .findByUserIdAndShiftDate(
                                template.getUser().getId(),
                                shiftDate
                        );
                if (!existingShifts.isEmpty()) {
                    shiftRepository.deleteAll(existingShifts);
                    shiftRepository.flush();
                }
            }

            Shift shift = Shift.builder()
                    .user(template.getUser())
                    .branch(template.getBranch())
                    .template(template)
                    .shiftDate(shiftDate)
                    .startTime(detail.getStartTime())
                    .endTime(detail.getEndTime())
                    .status(ShiftStatus.SCHEDULED)
                    .build();

            createdShifts.add(shiftRepository.save(shift));
        }

        return createdShifts.stream()
                .map(this::toShiftResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<ShiftResponse> applyAllTemplatesForBranch(
            Long branchId,
            ApplyTemplateRequest request
    ) {
        findBranch(branchId);
        List<ShiftTemplate> templates = templateRepository.findByBranchId(branchId);
        if (templates.isEmpty()) {
            return List.of();
        }

        LocalDate monday = request.getWeekStartDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        if (request.isOverwrite()) {
            List<Shift> existingShifts = shiftRepository
                    .findByBranchIdAndShiftDateBetween(branchId, monday, sunday);
            if (!existingShifts.isEmpty()) {
                shiftRepository.deleteAll(existingShifts);
                shiftRepository.flush();
            }
        }

        List<Shift> shiftsToSave = new ArrayList<>();

        for (ShiftTemplate template : templates) {
            for (ShiftTemplateDetail detail : template.getDetails()) {
                LocalDate shiftDate = monday.plusDays(detail.getDayOfWeek() - 1);

                if (!request.isOverwrite()) {
                    boolean hasOverlap = shiftRepository.existsOverlappingShift(
                            template.getUser().getId(),
                            template.getBranch().getId(),
                            shiftDate,
                            detail.getStartTime(),
                            detail.getEndTime(),
                            null
                    );
                    if (hasOverlap) {
                        continue;
                    }
                }

                Shift shift = Shift.builder()
                        .user(template.getUser())
                        .branch(template.getBranch())
                        .template(template)
                        .shiftDate(shiftDate)
                        .startTime(detail.getStartTime())
                        .endTime(detail.getEndTime())
                        .status(ShiftStatus.SCHEDULED)
                        .build();

                shiftsToSave.add(shift);
            }
        }

        List<Shift> savedShifts = shiftRepository.saveAll(shiftsToSave);

        return savedShifts.stream()
                .map(this::toShiftResponse)
                .toList();
    }

    // ── Query shifts ────────────────────────────────────────────

    @Override
    public List<ShiftResponse> getShiftsByUserAndWeek(
            Long userId,
            LocalDate weekStartDate
    ) {
        LocalDate monday = weekStartDate
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        return shiftRepository
                .findByUserIdAndShiftDateBetween(userId, monday, sunday)
                .stream()
                .map(this::toShiftResponse)
                .toList();
    }

    @Override
    public List<ShiftResponse> getShiftsByBranchAndDate(Long branchId, LocalDate date) {
        return shiftRepository.findByBranchIdAndShiftDate(branchId, date)
                .stream()
                .map(this::toShiftResponse)
                .toList();
    }

    @Override
    public List<ShiftResponse> getShiftsByBranchAndRange(Long branchId, LocalDate startDate, LocalDate endDate) {
        return shiftRepository.findByBranchIdAndShiftDateBetween(branchId, startDate, endDate)
                .stream()
                .map(this::toShiftResponse)
                .toList();
    }

    // ── Availability slots cho booking ──────────────────────────

    @Override
    public List<AvailabilitySlotResponse> getAvailabilitySlots(
            Long branchId,
            LocalDate date
    ) {
        List<Shift> scheduledShifts = shiftRepository
                .findScheduledShiftsByBranchAndDate(branchId, date);

        return scheduledShifts.stream()
                .map(shift -> AvailabilitySlotResponse.builder()
                        .userId(shift.getUser().getId())
                        .userName(shift.getUser().getFullName())
                        .date(shift.getShiftDate())
                        .startTime(shift.getStartTime())
                        .endTime(shift.getEndTime())
                        .available(true) // booking service sẽ update field này
                        .build())
                .toList();
    }

    // ── Helpers ────────────────────────────────────────────────

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
    }

    private Branch findBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with id: " + branchId));
    }

    private ShiftTemplate findTemplate(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shift template not found with id: " + templateId));
    }

    /**
     * Validate không có 2 detail nào trong cùng ngày bị overlap.
     */
    private void validateDetailsNoOverlap(List<ShiftTemplateDetailRequest> details) {
        for (int i = 0; i < details.size(); i++) {
            ShiftTemplateDetailRequest a = details.get(i);

            if (a.getEndTime().isBefore(a.getStartTime())
                    || a.getEndTime().equals(a.getStartTime())) {
                throw new IllegalArgumentException(
                        "Giờ kết thúc phải sau giờ bắt đầu (ngày "
                                + dayName(a.getDayOfWeek()) + ")");
            }

            for (int j = i + 1; j < details.size(); j++) {
                ShiftTemplateDetailRequest b = details.get(j);
                if (a.getDayOfWeek().equals(b.getDayOfWeek())
                        && a.getStartTime().isBefore(b.getEndTime())
                        && a.getEndTime().isAfter(b.getStartTime())) {
                    throw new IllegalArgumentException(
                            "Các ca trong template bị trùng giờ ở "
                                    + dayName(a.getDayOfWeek()));
                }
            }
        }
    }

    private List<ShiftTemplateDetail> buildDetails(
            List<ShiftTemplateDetailRequest> requests,
            ShiftTemplate template
    ) {
        return requests.stream()
                .map(req -> ShiftTemplateDetail.builder()
                        .template(template)
                        .dayOfWeek(req.getDayOfWeek())
                        .startTime(req.getStartTime())
                        .endTime(req.getEndTime())
                        .build())
                .toList();
    }

    private String dayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Thứ 2";
            case 2 -> "Thứ 3";
            case 3 -> "Thứ 4";
            case 4 -> "Thứ 5";
            case 5 -> "Thứ 6";
            case 6 -> "Thứ 7";
            case 7 -> "Chủ nhật";
            default -> "Ngày " + dayOfWeek;
        };
    }

    private ShiftTemplateDetailResponse toDetailResponse(ShiftTemplateDetail d) {
        return ShiftTemplateDetailResponse.builder()
                .id(d.getId())
                .dayOfWeek(d.getDayOfWeek())
                .dayName(dayName(d.getDayOfWeek()))
                .startTime(d.getStartTime())
                .endTime(d.getEndTime())
                .build();
    }

    private ShiftTemplateResponse toTemplateResponse(ShiftTemplate t) {
        return ShiftTemplateResponse.builder()
                .id(t.getId())
                .userId(t.getUser().getId())
                .userName(t.getUser().getFullName())
                .branchId(t.getBranch().getId())
                .branchName(t.getBranch().getName())
                .name(t.getName())
                .description(t.getDescription())
                .isActive(t.getIsActive())
                .details(t.getDetails().stream()
                        .map(this::toDetailResponse)
                        .toList())
                .build();
    }

    private ShiftResponse toShiftResponse(Shift s) {
        return ShiftResponse.builder()
                .id(s.getId())
                .userId(s.getUser().getId())
                .userName(s.getUser().getFullName())
                .branchId(s.getBranch().getId())
                .branchName(s.getBranch().getName())
                .templateId(s.getTemplate() != null ? s.getTemplate().getId() : null)
                .shiftDate(s.getShiftDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus().name())
                .note(s.getNote())
                .build();
    }
}
