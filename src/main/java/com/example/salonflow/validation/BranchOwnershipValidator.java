package com.example.salonflow.validation;

import org.springframework.stereotype.Component;

import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.exception.BusinessAccessDeniedException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BranchOwnershipValidator {

    private final BranchRepository branchRepository;

    private final SalonRepository salonRepository;

    public Branch validateOwnerBranch(
            Long branchId
    ) {

        Long ownerId =
                SecurityUtils.getCurrentUserId();

        Salon salon =
                salonRepository
                        .findFirstByOwnerId(ownerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Salon not found"
                                )
                        );

        Branch branch =
                branchRepository
                        .findById(branchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Branch not found"
                                )
                        );

        if (!branch.getSalon().getId().equals(salon.getId())) {
            throw new BusinessAccessDeniedException(
                    "Branch does not belong to your salon"
            );
        }

        return branch;
    }
}