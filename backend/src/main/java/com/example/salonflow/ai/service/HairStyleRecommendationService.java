package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.hair.HairStyleAnalysisResult;
import com.example.salonflow.ai.dto.hair.HairStyleRecommendationItem;

import java.util.List;

public interface HairStyleRecommendationService {

    List<HairStyleRecommendationItem> recommend(HairStyleAnalysisResult analysis, int limit);
}
