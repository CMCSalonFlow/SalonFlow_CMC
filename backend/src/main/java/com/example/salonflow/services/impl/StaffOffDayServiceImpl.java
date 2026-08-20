package com.example.salonflow.services.impl;

import com.example.salonflow.dto.offday.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.entity.enums.BookingStatus;
import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.StaffOffDayService;
import com.example.salonflow.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffOffDayServiceImpl implements StaffOffDayService {

    private final StaffOffDayRepository offDayRepository;
    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SalonRepository salonRepository;
    private final UserBranchRepository userBranchRepository;

    @Override
    public StaffLeaveResponse createLeaveRequest(Long userId, CreateLeaveRequest request) {
        if (request.getDateFrom() == null || request.getDateTo() == null) {
            throw new BadRequestException("Vui lòng chọn ngày bắt đầu và ngày kết thúc");
        }

        if (request.getDateFrom().isAfter(request.getDateTo())) {
            throw new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }

        Staff staff = staffRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhân viên liên kết với tài khoản này"));

        List<StaffOffDay> overlapping = offDayRepository.findOverlappingActiveRequests(
                staff.getId(), request.getDateFrom(), request.getDateTo(), null);

        if (!overlapping.isEmpty()) {
            throw new BadRequestException("Khoảng thời gian đăng ký xin nghỉ bị trùng với một đơn nghỉ khác của bạn");
        }

        boolean isOwner = isOwnerUser(userId);

        StaffOffDay offDay = StaffOffDay.builder()
                .staff(staff)
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .leaveType(request.getLeaveType() != null ? request.getLeaveType() : LeaveType.PERSONAL)
                .status(isOwner ? LeaveStatus.APPROVED : LeaveStatus.PENDING)
                .reason(request.getReason())
                .createdBy(SecurityUtil.getCurrentUsername())
                .approvedBy(isOwner ? userId : null)
                .approvedAt(isOwner ? Instant.now() : null)
                .build();

        StaffOffDay saved = offDayRepository.save(offDay);

        if (saved.getStatus() == LeaveStatus.APPROVED) {
            cancelConflictingBookings(staff.getId(), request.getDateFrom(), request.getDateTo());
        }

        return convertToLeaveResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffLeaveResponse> getMyLeaveRequests(Long userId) {
        Staff staff = staffRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhân viên của tài khoản này"));

        List<StaffOffDay> list = offDayRepository.findByStaffIdOrderByCreatedAtDesc(staff.getId());
        return list.stream().map(offDay -> convertToLeaveResponse(offDay)).toList();
    }

    @Override
    public void cancelLeaveRequest(Long userId, Long leaveId) {
        StaffOffDay offDay = offDayRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn xin nghỉ phép"));

        Long applicantUserId = (offDay.getStaff() != null) ? offDay.getStaff().getUserId() : null;
        if (!java.util.Objects.equals(applicantUserId, userId) && !isOwnerUser(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy đơn xin nghỉ này");
        }

        if (offDay.getStatus() == LeaveStatus.APPROVED) {
            throw new BadRequestException("Đơn nghỉ đã được duyệt, không thể tự hủy. Vui lòng liên hệ Quản lý");
        }

        offDay.setStatus(LeaveStatus.CANCELLED);
        offDayRepository.save(offDay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffLeaveResponse> getApprovalList(Long userId, LeaveStatus status, Long branchId) {
        boolean isOwner = isOwnerUser(userId);

        if (isOwner) {
            Salon salon = salonRepository.findFirstByOwnerId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Salon của bạn"));
            List<StaffOffDay> list = offDayRepository.findOffDaysForOwner(salon.getId(), branchId, status);
            return list.stream().map(offDay -> convertToLeaveResponse(offDay)).toList();
        }

        // Nếu là Manager
        List<UserBranch> userBranches = userBranchRepository.findByUser_Id(userId);
        if (userBranches.isEmpty()) {
            throw new BadRequestException("Bạn chưa được gán quản lý chi nhánh nào");
        }

        Long managerBranchId = userBranches.get(0).getBranch().getId();
        List<StaffOffDay> list = offDayRepository.findOffDaysForManager(managerBranchId, status);
        return list.stream().map(offDay -> convertToLeaveResponse(offDay)).toList();
    }

    @Override
    public StaffLeaveResponse approveLeaveRequest(Long userId, Long leaveId) {
        StaffOffDay offDay = offDayRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn xin nghỉ phép"));

        validateApproverRights(userId, offDay);

        offDay.setStatus(LeaveStatus.APPROVED);
        offDay.setApprovedBy(userId);
        offDay.setApprovedAt(Instant.now());

        StaffOffDay updated = offDayRepository.save(offDay);

        cancelConflictingBookings(offDay.getStaff().getId(), offDay.getDateFrom(), offDay.getDateTo());

        return convertToLeaveResponse(updated);
    }

    @Override
    public StaffLeaveResponse rejectLeaveRequest(Long userId, Long leaveId, RejectLeaveRequest request) {
        StaffOffDay offDay = offDayRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn xin nghỉ phép"));

        validateApproverRights(userId, offDay);

        offDay.setStatus(LeaveStatus.REJECTED);
        offDay.setRejectionReason(request.getRejectionReason());
        offDay.setApprovedBy(userId);
        offDay.setApprovedAt(Instant.now());

        StaffOffDay updated = offDayRepository.save(offDay);
        return convertToLeaveResponse(updated);
    }

    // Helper: validate permission of approver
    private void validateApproverRights(Long userId, StaffOffDay offDay) {
        boolean isOwner = isOwnerUser(userId);
        if (isOwner) {
            return; // Owner has full rights
        }

        // If Manager:
        List<UserBranch> userBranches = userBranchRepository.findByUser_Id(userId);
        if (userBranches.isEmpty()) {
            throw new BadRequestException("Bạn không có quyền duyệt đơn này");
        }

        Long managerBranchId = userBranches.get(0).getBranch().getId();
        Long staffBranchId = offDay.getStaff().getBranch() != null ? offDay.getStaff().getBranch().getId() : null;

        if (!managerBranchId.equals(staffBranchId)) {
            throw new BadRequestException("Bạn chỉ có quyền duyệt đơn của nhân viên thuộc chi nhánh bạn quản lý");
        }

        // If the applicant is a Manager, only Owner can approve
        boolean applicantIsManager = isManagerUser(offDay.getStaff().getUserId());
        if (applicantIsManager) {
            throw new BadRequestException("Chỉ có Salon Owner mới có quyền duyệt đơn xin nghỉ phép của Quản lý chi nhánh");
        }
    }

    private boolean isOwnerUser(Long userId) {
        if (userId == null) return false;

        if (salonRepository.findFirstByOwnerId(userId).isPresent()) {
            return true;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getUserRoles() == null) return false;
        return user.getUserRoles().stream().anyMatch(ur -> {
            if (ur.getRole() == null) return false;
            String code = ur.getRole().getCode() != null ? ur.getRole().getCode().toUpperCase() : "";
            String name = ur.getRole().getName() != null ? ur.getRole().getName().toUpperCase() : "";
            return code.contains("OWNER") || name.contains("OWNER") || name.contains("CHỦ");
        });
    }

    private boolean isManagerUser(Long userId) {
        if (userId == null) return false;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getUserRoles() == null) return false;
        return user.getUserRoles().stream().anyMatch(ur -> {
            if (ur.getRole() == null) return false;
            String code = ur.getRole().getCode() != null ? ur.getRole().getCode().toUpperCase() : "";
            return "MANAGER".equals(code) || "ROLE_MANAGER".equals(code) ||
                   "BRANCH_MANAGER".equals(code) || "ROLE_BRANCH_MANAGER".equals(code);
        });
    }

    // Legacy methods
    @Override
    public StaffOffDayResponse createOffDay(Long staffId, StaffOffDayRequest request) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + staffId));

        StaffOffDay offDay = StaffOffDay.builder()
                .staff(staff)
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .leaveType(LeaveType.PERSONAL)
                .status(LeaveStatus.APPROVED)
                .reason(request.getReason())
                .createdBy(SecurityUtil.getCurrentUsername())
                .approvedAt(Instant.now())
                .build();

        StaffOffDay saved = offDayRepository.save(offDay);
        cancelConflictingBookings(staffId, request.getDateFrom(), request.getDateTo());

        return convertToLegacyResponse(saved);
    }

    @Override
    public StaffOffDayResponse updateOffDay(Long offDayId, StaffOffDayRequest request) {
        StaffOffDay offDay = offDayRepository.findById(offDayId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nghỉ"));

        offDay.setDateFrom(request.getDateFrom());
        offDay.setDateTo(request.getDateTo());
        offDay.setReason(request.getReason());

        StaffOffDay updated = offDayRepository.save(offDay);
        return convertToLegacyResponse(updated);
    }

    @Override
    public void deleteOffDay(Long offDayId) {
        StaffOffDay offDay = offDayRepository.findById(offDayId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nghỉ"));
        offDayRepository.delete(offDay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffOffDayResponse> getOffDaysByStaffId(Long staffId) {
        List<StaffOffDay> offDays = offDayRepository.findByStaffIdOrderByCreatedAtDesc(staffId);
        return offDays.stream().map(this::convertToLegacyResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffOffDayResponse> getOffDaysInRange(Long staffId, LocalDate startDate, LocalDate endDate) {
        List<StaffOffDay> offDays = offDayRepository.findApprovedOffDaysInRange(staffId, startDate, endDate);
        return offDays.stream().map(this::convertToLegacyResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStaffOffInPeriod(Long staffId, LocalDate date) {
        return offDayRepository.isStaffApprovedOffOnDate(staffId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStaffOffInPeriod(Long staffId, LocalDate startDate, LocalDate endDate) {
        List<StaffOffDay> list = offDayRepository.findApprovedOffDaysInRange(staffId, startDate, endDate);
        return !list.isEmpty();
    }

    private void cancelConflictingBookings(Long staffId, LocalDate fromDate, LocalDate toDate) {
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookingsWithOffDay(
                staffId, fromDate, toDate);

        for (Booking booking : conflictingBookings) {
            if (booking.getStatus() != BookingStatus.CANCELLED) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setNotes("Đã bị hủy tự động vì nhân viên nghỉ phép từ " 
                        + fromDate + " đến " + toDate);
            }
        }

        if (!conflictingBookings.isEmpty()) {
            bookingRepository.saveAll(conflictingBookings);
        }
    }

    private StaffLeaveResponse convertToLeaveResponse(StaffOffDay offDay) {
        Staff staff = offDay.getStaff();
        Branch branch = staff != null ? staff.getBranch() : null;

        long days = 1;
        if (offDay.getDateFrom() != null && offDay.getDateTo() != null) {
            days = ChronoUnit.DAYS.between(offDay.getDateFrom(), offDay.getDateTo()) + 1;
        }

        String approverName = null;
        if (offDay.getApprovedBy() != null) {
            User approver = userRepository.findById(offDay.getApprovedBy()).orElse(null);
            if (approver != null) {
                approverName = approver.getFullName() != null ? approver.getFullName() : approver.getUsername();
            }
        }

        String staffRole = "Nhân viên";
        if (staff != null && staff.getUserId() != null) {
            if (isOwnerUser(staff.getUserId())) {
                staffRole = "Chủ Salon (Owner)";
            } else if (isManagerUser(staff.getUserId())) {
                staffRole = "Quản lý Chi nhánh (Manager)";
            } else {
                staffRole = "Nhân viên";
            }
        }

        return StaffLeaveResponse.builder()
                .id(offDay.getId())
                .staffId(staff != null ? staff.getId() : null)
                .staffUserId(staff != null ? staff.getUserId() : null)
                .staffName(staff != null ? staff.getName() : null)
                .staffAvatar(staff != null ? staff.getAvatarUrl() : null)
                .staffRole(staffRole)
                .branchId(branch != null ? branch.getId() : null)
                .branchName(branch != null ? branch.getName() : "Chưa gán chi nhánh")
                .dateFrom(offDay.getDateFrom())
                .dateTo(offDay.getDateTo())
                .totalDays(days)
                .leaveType(offDay.getLeaveType() != null ? offDay.getLeaveType() : LeaveType.PERSONAL)
                .status(offDay.getStatus() != null ? offDay.getStatus() : LeaveStatus.PENDING)
                .reason(offDay.getReason())
                .rejectionReason(offDay.getRejectionReason())
                .approvedBy(offDay.getApprovedBy())
                .approvedByName(approverName)
                .approvedAt(offDay.getApprovedAt())
                .createdAt(offDay.getCreatedAt())
                .build();
    }

    @Override
    public List<StaffLeaveResponse> getApprovedLeaves(Long branchId, LocalDate startDate, LocalDate endDate) {
        List<StaffOffDay> list;
        if (branchId != null) {
            list = offDayRepository.findByStaff_Branch_IdAndStatusAndDateToGreaterThanEqualAndDateFromLessThanEqual(
                    branchId, LeaveStatus.APPROVED, startDate, endDate);
        } else {
            list = offDayRepository.findByStatusAndDateToGreaterThanEqualAndDateFromLessThanEqual(
                    LeaveStatus.APPROVED, startDate, endDate);
        }
        return list.stream().map(offDay -> convertToLeaveResponse(offDay)).toList();
    }

    private StaffOffDayResponse convertToLegacyResponse(StaffOffDay offDay) {
        Staff staff = offDay.getStaff();
        return StaffOffDayResponse.builder()
                .id(offDay.getId())
                .staffId(staff.getId())
                .staffName(staff.getName())
                .dateFrom(offDay.getDateFrom())
                .dateTo(offDay.getDateTo())
                .reason(offDay.getReason())
                .createdBy(offDay.getCreatedBy())
                .createdAt(offDay.getCreatedAt() != null ? offDay.getCreatedAt().toString() : null)
                .build();
    }
}