package com.example.salonflow.ai.provider;

import com.example.salonflow.ai.dto.hair.HairVisionAnalysisRequest;
import com.example.salonflow.ai.dto.hair.HairVisionAnalysisResult;

public interface HairVisionProvider {

    String providerName();

    HairVisionAnalysisResult analyze(HairVisionAnalysisRequest request);
}
