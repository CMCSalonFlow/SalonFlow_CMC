package com.example.salonflow.ai.controller;

import com.example.salonflow.ai.dto.hair.HairStyleAnalyzeRequest;
import com.example.salonflow.ai.dto.hair.HairStyleConfirmRequest;
import com.example.salonflow.ai.dto.hair.HairStyleProfileResponse;
import com.example.salonflow.ai.dto.hair.HairStyleRecommendationResponse;
import com.example.salonflow.ai.service.HairStyleAnalysisService;
import com.example.salonflow.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hair-styles")
@RequiredArgsConstructor
public class HairStyleAiController {

    private final HairStyleAnalysisService hairStyleAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<HairStyleRecommendationResponse> analyze(
            @Valid @RequestBody HairStyleAnalyzeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(hairStyleAnalysisService.analyze(userId, request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<HairStyleProfileResponse> confirm(
            @RequestBody HairStyleConfirmRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(hairStyleAnalysisService.confirmSelection(userId, request));
    }

    @GetMapping("/profile")
    public ResponseEntity<HairStyleProfileResponse> profile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(hairStyleAnalysisService.getProfile(userId));
    }
}
