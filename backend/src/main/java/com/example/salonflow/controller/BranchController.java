package com.example.salonflow.controller;

import com.example.salonflow.dto.Branch.CreateBranchRequest;
import com.example.salonflow.dto.Branch.UpdateBranchRequest;
import com.example.salonflow.dto.Branch.BranchResponse;
import com.example.salonflow.dto.Branch.BranchSummaryResponse;
import com.example.salonflow.services.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping("/my-branches")
    public ResponseEntity<List<BranchSummaryResponse>>
    getMyBranches() {

        return ResponseEntity.ok(
                branchService.getMyBranches()
        );
    }
}