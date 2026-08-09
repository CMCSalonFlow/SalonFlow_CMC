package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.hair.HairStyleAnalyzeRequest;
import com.example.salonflow.ai.dto.hair.HairStyleConfirmRequest;
import com.example.salonflow.ai.dto.hair.HairStyleProfileResponse;
import com.example.salonflow.ai.dto.hair.HairStyleRecommendationResponse;

public interface HairStyleAnalysisService {

    HairStyleRecommendationResponse analyze(Long userId, HairStyleAnalyzeRequest request);

    HairStyleProfileResponse getProfile(Long userId);

    HairStyleProfileResponse confirmSelection(Long userId, HairStyleConfirmRequest request);
}
