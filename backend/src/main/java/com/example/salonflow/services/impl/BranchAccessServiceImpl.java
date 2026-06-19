package com.example.salonflow.services.impl;
import org.springframework.stereotype.Service;

import com.example.salonflow.exception.BusinessAccessDeniedException;
import com.example.salonflow.repository.UserBranchRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.BranchAccessService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class BranchAccessServiceImpl
        implements BranchAccessService {

    private final UserBranchRepository userBranchRepository;

    @Override
    public void validateCurrentBranchAccess() {

        Long userId =
                SecurityUtils.getCurrentUserId();

        Long branchId =
                SecurityUtils.getCurrentBranchId();

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