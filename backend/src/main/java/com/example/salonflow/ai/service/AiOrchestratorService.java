package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.chat.AiChatRequest;
import com.example.salonflow.ai.dto.chat.AiChatResponse;

public interface AiOrchestratorService {

    AiChatResponse execute(AiChatRequest request);
}

