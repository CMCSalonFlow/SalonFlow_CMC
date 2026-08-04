package com.example.salonflow.ai.prompt;

import com.example.salonflow.ai.dto.description.ServiceDescriptionGenerateRequest;

public interface ServiceDescriptionPromptBuilder {

    String buildPrompt(ServiceDescriptionGenerateRequest request);
}
