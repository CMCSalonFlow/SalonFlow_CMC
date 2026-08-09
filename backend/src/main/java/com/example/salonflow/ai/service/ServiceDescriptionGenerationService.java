package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.description.ServiceDescriptionGenerateRequest;
import com.example.salonflow.ai.dto.description.ServiceDescriptionGenerateResponse;
import com.example.salonflow.ai.dto.description.ServiceDescriptionQuotaResponse;

public interface ServiceDescriptionGenerationService {

    ServiceDescriptionGenerateResponse generate(Long ownerId, ServiceDescriptionGenerateRequest request);

    ServiceDescriptionQuotaResponse getQuota(Long ownerId, Long salonId);
}
