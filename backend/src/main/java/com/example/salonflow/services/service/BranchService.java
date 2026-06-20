package com.example.salonflow.services.service;

import java.util.List;

import com.example.salonflow.dto.Branch.BranchResponse;
import com.example.salonflow.dto.Branch.BranchSummaryResponse;
import com.example.salonflow.dto.Branch.CreateBranchRequest;
import com.example.salonflow.dto.Branch.UpdateBranchRequest;
import com.example.salonflow.dto.Branch.UserInBranchResponse;
public interface BranchService {

    List<BranchSummaryResponse> getMyBranches();

    BranchResponse create(
            CreateBranchRequest request
    );

    BranchResponse update(
            Long branchId,
            UpdateBranchRequest request
    );

    void delete(
            Long branchId
    );

    List<BranchResponse> getAll();

    BranchResponse getById(
            Long branchId
    );

    void assignUser(
            Long branchId,
            Long userId
    );

    void removeUser(
            Long branchId,
            Long userId
    );

    List<UserInBranchResponse> getUsers(
            Long branchId
    );
}