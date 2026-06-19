package com.example.salonflow.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.salonflow.security.SecurityUtils;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/context")
    public Map<String, Object> context() {

        return Map.of(
                "userId",
                SecurityUtils.getCurrentUserId(),
                "branchId",
                SecurityUtils.getCurrentBranchId()
        );
    }
}
