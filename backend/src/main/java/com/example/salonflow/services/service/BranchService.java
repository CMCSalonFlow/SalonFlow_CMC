package com.example.salonflow.services.service;


import com.example.salonflow.dto.Branch.CreateBranchRequest;
import com.example.salonflow.dto.Branch.UpdateBranchRequest;
import com.example.salonflow.dto.Branch.BranchResponse;

import java.util.List;

public interface BranchService {

    BranchResponse create(
            Long salonId,
            CreateBranchRequest request
    );

    List<BranchResponse> getBySalon(
            Long salonId
    );

    BranchResponse update(
            Long branchId,
            UpdateBranchRequest request
    );

    void delete(Long branchId);
}