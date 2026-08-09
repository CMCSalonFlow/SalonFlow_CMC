package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.hair.HairStyleAnalysisResult;
import com.example.salonflow.ai.dto.hair.HairStyleRecommendationItem;
import com.example.salonflow.entity.enums.hair.HairGender;

import java.util.List;

public interface HairStyleRecommendationService {

    List<HairStyleRecommendationItem> recommend(HairStyleAnalysisResult analysis, HairGender gender, int limit);
}
