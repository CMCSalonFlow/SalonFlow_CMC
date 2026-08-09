package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.description.ServiceDescriptionQuotaResponse;

public interface ServiceDescriptionQuotaService {

    ServiceDescriptionQuotaResponse getQuota(Long salonId);

    ServiceDescriptionQuotaResponse consumeQuota(Long salonId);
}
