package com.example.salonflow.services.impl;

import com.example.salonflow.dto.Branch.CreateBranchRequest;
import com.example.salonflow.dto.Branch.UpdateBranchRequest;
import com.example.salonflow.dto.Branch.BranchResponse;
import com.example.salonflow.dto.Branch.BranchSummaryResponse;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.User;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.repository.UserBranchRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.BranchService;
import com.example.salonflow.services.service.UserService;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.exception.BusinessAccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final UserBranchRepository userBranchRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BranchSummaryResponse> getMyBranches() {

        Long userId =
                SecurityUtils.getCurrentUserId();

        return userBranchRepository
                .findByUser_Id(userId)
                .stream()
                .map(userBranch -> {

                    Branch branch =
                            userBranch.getBranch();

                    return BranchSummaryResponse
                            .builder()
                            .id(branch.getId())
                            .name(branch.getName())
                            .address(branch.getAddress())
                            .isActive(branch.getIsActive())
                            .build();
                })
                .toList();
    }
}