package com.example.salonflow.services.impl;
import org.springframework.stereotype.Service;

import com.example.salonflow.exception.BusinessAccessDeniedException;
import com.example.salonflow.repository.UserBranchRepository;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.BranchAccessService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class BranchAccessServiceImpl
        implements BranchAccessService {

    private final UserBranchRepository userBranchRepository;
    private final BranchRepository branchRepository;

    @Override
    public void validateCurrentBranchAccess() {

        Long userId =
                SecurityUtils.getCurrentUserId();

        Long branchId =
                SecurityUtils.getCurrentBranchId();

        // 1. Nếu user là Owner của Salon có chi nhánh này, tự động cho phép truy cập
        Branch branch = branchRepository.findById(branchId).orElse(null);
        if (branch != null && branch.getSalon() != null && branch.getSalon().getOwner() != null) {
            if (branch.getSalon().getOwner().getId().equals(userId)) {
                return;
            }
        }

        // 2. Ngược lại, kiểm tra phân quyền trong user_branches (ví dụ: Staff)
        boolean allowed =
                userBranchRepository
                        .existsByUser_IdAndBranch_Id(
                                userId,
                                branchId
                        );

        if (!allowed) {

            throw new BusinessAccessDeniedException(
                    "You do not have access to this branch"
            );
        }
    }
}