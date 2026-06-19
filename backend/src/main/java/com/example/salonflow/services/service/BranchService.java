package com.example.salonflow.services.service;

import java.util.List;

import com.example.salonflow.dto.Branch.BranchSummaryResponse;

public interface BranchService {

    List<BranchSummaryResponse> getMyBranches();
}