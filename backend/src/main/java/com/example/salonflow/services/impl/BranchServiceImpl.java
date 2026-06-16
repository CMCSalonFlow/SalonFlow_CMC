package com.example.salonflow.services.impl;

import com.example.salonflow.dto.Branch.CreateBranchRequest;
import com.example.salonflow.dto.Branch.UpdateBranchRequest;
import com.example.salonflow.dto.Branch.BranchResponse;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.User;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.BranchService;
import com.example.salonflow.services.service.UserService;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.exception.BusinessAccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final SalonRepository salonRepository;
    private final UserService userService;
    @Override
    public BranchResponse create(
            Long salonId,
            CreateBranchRequest request
    ) {

        User currentUser =
                userService.getCurrentUser();

        Salon salon =
                salonRepository
                        .findByIdAndOwnerId(
                                salonId,
                                currentUser.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Salon with id " + salonId + " not found"
                                ));

        Branch branch = Branch.builder()
                .salon(salon)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .isActive(true)
                .build();

        branchRepository.save(branch);

        return map(branch);
    }

    @Override
    public List<BranchResponse> getBySalon(
            Long salonId
    ) {

        User currentUser =
                userService.getCurrentUser();

        salonRepository
                .findByIdAndOwnerId(
                        salonId,
                        currentUser.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Salon with id " + salonId + " not found"
                        ));

        return branchRepository
                .findBySalonId(salonId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public BranchResponse update(
            Long branchId,
            UpdateBranchRequest request
    ) {

        Branch branch =
                branchRepository
                        .findById(branchId)
                        .orElseThrow();

        branch.setName(request.getName());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());
        branch.setAddress(request.getAddress());
        branch.setIsActive(request.getIsActive());

        branchRepository.save(branch);

        return map(branch);
    }

    @Override
    public void delete(Long branchId) {

        Branch branch =
                branchRepository
                        .findById(branchId)
                        .orElseThrow();

        branchRepository.delete(branch);
    }

    private BranchResponse map(
            Branch branch
    ) {
        return BranchResponse.builder()
                .id(branch.getId())
                .salonId(branch.getSalon().getId())
                .name(branch.getName())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .address(branch.getAddress())
                .isActive(branch.getIsActive())
                .build();
    }
}