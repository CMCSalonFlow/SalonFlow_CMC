package com.example.salonflow.services.impl;

import com.example.salonflow.dto.Branch.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.salonflow.validation.BranchOwnershipValidator;
import com.example.salonflow.search.service.BranchSearchService;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

        private final BranchRepository branchRepository;

        private final SalonRepository salonRepository;

        private final UserRepository userRepository;

        private final BranchSearchService branchSearchService;

        private final UserBranchRepository userBranchRepository;

        private final BranchOwnershipValidator branchOwnershipValidator;

        private final BranchHourRepository branchHourRepository;

        @Override
        @Transactional(readOnly = true)
        public List<BranchSummaryResponse> getMyBranches() {

                Long userId = SecurityUtils.getCurrentUserId();

                // 1. Nếu user là Owner của Salon, trả về toàn bộ chi nhánh của Salon đó
                java.util.Optional<com.example.salonflow.entity.Salon> salonOpt = salonRepository
                                .findFirstByOwnerId(userId);
                if (salonOpt.isPresent()) {
                        return branchRepository.findBySalonId(salonOpt.get().getId())
                                        .stream()
                                        .map(branch -> BranchSummaryResponse.builder()
                                                        .id(branch.getId())
                                                        .name(branch.getName())
                                                        .address(branch.getAddress())
                                                        .latitude(branch.getLatitude())
                                                        .longitude(branch.getLongitude())
                                                        .isActive(branch.getIsActive())
                                                        .build())
                                        .toList();
                }

                // 2. Nếu không phải Owner (ví dụ: Staff), trả về các chi nhánh được gán trong
                // user_branches
                List<BranchSummaryResponse> assignedBranches = userBranchRepository
                                .findByUser_Id(userId)
                                .stream()
                                .map(userBranch -> {

                                        Branch branch = userBranch.getBranch();

                                        return BranchSummaryResponse
                                                        .builder()
                                                        .id(branch.getId())
                                                        .name(branch.getName())
                                                        .address(branch.getAddress())
                                                        .latitude(branch.getLatitude())
                                                        .longitude(branch.getLongitude())
                                                        .isActive(branch.getIsActive())
                                                        .build();
                                })
                                .toList();

                if (!assignedBranches.isEmpty()) {
                        return assignedBranches;
                }

                // 3. Nếu là Khách hàng (Customer) hoặc không có chi nhánh nào được gán, trả về
                // tất cả chi nhánh active trong hệ thống
                // Bảo mật: Nếu user là Salon Owner hoặc Staff nhưng không có chi nhánh nào
                // thuộc về họ, trả về danh sách rỗng
                boolean isOwnerOrStaff = org.springframework.security.core.context.SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getAuthorities()
                                .stream()
                                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                                .anyMatch(authority -> authority.equals("ROLE_SALON_OWNER")
                                                || authority.equals("ROLE_STAFF"));

                if (isOwnerOrStaff) {
                        return List.of();
                }

                return branchRepository.findAll()
                                .stream()
                                .filter(Branch::getIsActive)
                                .map(branch -> BranchSummaryResponse.builder()
                                                .id(branch.getId())
                                                .name(branch.getName())
                                                .address(branch.getAddress())
                                                .latitude(branch.getLatitude())
                                                .longitude(branch.getLongitude())
                                                .isActive(branch.getIsActive())
                                                .build())
                                .toList();
        }

        @Override
        @Transactional
        public BranchResponse create(
                        CreateBranchRequest request) {

                Long ownerId = SecurityUtils.getCurrentUserId();

                Salon salon = salonRepository
                                .findFirstByOwnerId(ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Salon not found"));

                Branch branch = Branch.builder()
                                .name(request.getName())
                                .phone(request.getPhone())
                                .email(request.getEmail())
                                .address(request.getAddress())
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .isActive(true)
                                .salon(salon)
                                .build();

                Branch saved = branchRepository.save(branch);

                saveHours(saved, request.getHours());

                branchSearchService.indexBranch(saved.getId());
                User owner = userRepository
                                .findById(ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Owner not found"));

                UserBranch userBranch = UserBranch.builder()
                                .id(
                                                new UserBranchId(
                                                                owner.getId(),
                                                                saved.getId()))
                                .user(owner)
                                .branch(saved)
                                .assignedAt(
                                                Instant.now())
                                .build();

                userBranchRepository.save(userBranch);

                return mapToResponse(saved);
        }

        @Override
        @Transactional
        public BranchResponse update(
                        Long branchId,
                        UpdateBranchRequest request) {

                Branch branch = branchOwnershipValidator
                                .validateOwnerBranch(
                                                branchId);

                branch.setName(
                                request.getName());

                branch.setPhone(
                                request.getPhone());

                branch.setEmail(
                                request.getEmail());

                branch.setAddress(
                                request.getAddress());

                branch.setLatitude(
                                request.getLatitude());

                branch.setLongitude(
                                request.getLongitude());

                branch.setIsActive(
                                request.getIsActive());

                Branch saved = branchRepository.save(branch);
                branchHourRepository.deleteByBranch(saved);
                branchHourRepository.flush();
                saved.getHours().clear();
                saveHours(saved, request.getHours());

                return mapToResponse(saved);
        }

        @Override
        @Transactional
        public void delete(
                        Long branchId) {

                Branch branch = branchOwnershipValidator
                                .validateOwnerBranch(
                                                branchId);
                branch.setIsActive(false);

                Branch updated = branchRepository.save(branch);

                branchSearchService.indexBranch(updated.getId());
        }

        @Override
        @Transactional(readOnly = true)
        public List<BranchResponse> getAll() {

                Long ownerId = SecurityUtils.getCurrentUserId();

                Salon salon = salonRepository
                                .findFirstByOwnerId(ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Salon not found"));

                return branchRepository
                                .findBySalonId(
                                                salon.getId())
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public BranchResponse getById(
                        Long branchId) {

                Branch branch = branchOwnershipValidator
                                .validateOwnerBranch(
                                                branchId);

                return mapToResponse(branch);
        }

        @Override
        @Transactional
        public void assignUser(
                        Long branchId,
                        Long userId) {

                Branch branch = branchOwnershipValidator
                                .validateOwnerBranch(
                                                branchId);

                User user = userRepository
                                .findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User not found"));

                if (userBranchRepository
                                .existsByUser_IdAndBranch_Id(
                                                userId,
                                                branchId)) {
                        return;
                }

                UserBranch userBranch = UserBranch.builder()
                                .id(
                                                new UserBranchId(
                                                                userId,
                                                                branchId))
                                .user(user)
                                .branch(branch)
                                .assignedAt(
                                                Instant.now())
                                .build();

                userBranchRepository.save(userBranch);
        }

        @Override
        @Transactional
        public void removeUser(
                        Long branchId,
                        Long userId) {

                branchOwnershipValidator
                                .validateOwnerBranch(
                                                branchId);

                userBranchRepository
                                .deleteByUser_IdAndBranch_Id(
                                                userId,
                                                branchId);
        }

        @Override
        @Transactional(readOnly = true)
        public List<UserInBranchResponse> getUsers(
                        Long branchId) {

                branchOwnershipValidator
                                .validateOwnerBranch(
                                                branchId);

                return userBranchRepository
                                .findAllUsersByBranchId(branchId)
                                .stream()
                                .map(userBranch -> {

                                        User user = userBranch.getUser();

                                        return UserInBranchResponse
                                                        .builder()
                                                        .id(user.getId())
                                                        .fullName(user.getFullName())
                                                        .email(user.getEmail())
                                                        .phone(user.getPhone())
                                                        .build();
                                })
                                .toList();
        }

        private BranchResponse mapToResponse(
                        Branch branch) {
                List<BranchHourResponse> hourResponses = branch.getHours() != null
                                ? branch.getHours().stream()
                                                .map(hour -> BranchHourResponse.builder()
                                                                .dayOfWeek(hour.getDayOfWeek())
                                                                .openTime(hour.getOpenTime())
                                                                .closeTime(hour.getCloseTime())
                                                                .isClosed(hour.getIsClosed())
                                                                .build())
                                                .toList()
                                : new java.util.ArrayList<>();

                return BranchResponse
                                .builder()
                                .id(branch.getId())
                                .name(branch.getName())
                                .salonId(branch.getSalon().getId())
                                .phone(branch.getPhone())
                                .email(branch.getEmail())
                                .address(branch.getAddress())
                                .latitude(branch.getLatitude())
                                .longitude(branch.getLongitude())
                                .isActive(branch.getIsActive())
                                .hours(hourResponses)
                                .build();
        }

        private void saveHours(
                        Branch branch,
                        List<BranchHourRequest> requests) {
                if (requests == null || requests.isEmpty()) {
                        List<BranchHour> defaultHours = new java.util.ArrayList<>();
                        for (int i = 0; i < 7; i++) {
                                defaultHours.add(BranchHour.builder()
                                                .branch(branch)
                                                .dayOfWeek(i)
                                                .openTime(java.time.LocalTime.of(9, 0))
                                                .closeTime(java.time.LocalTime.of(21, 0))
                                                .isClosed(false)
                                                .build());
                        }
                        branchHourRepository.saveAll(defaultHours);
                        branch.getHours().addAll(defaultHours);
                        return;
                }

                List<BranchHour> hours = new java.util.ArrayList<>();
                for (BranchHourRequest request : requests) {
                        BranchHour hour = BranchHour.builder()
                                        .branch(branch)
                                        .dayOfWeek(request.getDayOfWeek())
                                        .openTime(request.getOpenTime())
                                        .closeTime(request.getCloseTime())
                                        .isClosed(Boolean.TRUE.equals(request.getIsClosed()))
                                        .build();
                        hours.add(hour);
                }
                branchHourRepository.saveAll(hours);
                branch.getHours().addAll(hours);
        }
}
