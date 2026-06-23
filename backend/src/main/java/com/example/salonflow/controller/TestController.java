package com.example.salonflow.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.salonflow.security.SecurityUtils;
import com.example.salonflow.services.service.BranchAccessService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final BranchAccessService branchAccessService;

    @GetMapping("/context")
    public Map<String, Object> context() {

        branchAccessService
                .validateCurrentBranchAccess();

        return Map.of(
                "userId",
                SecurityUtils.getCurrentUserId(),
                "branchId",
                SecurityUtils.getCurrentBranchId()
        );
    }
}
