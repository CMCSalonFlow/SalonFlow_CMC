package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.dto.chat.AiChatRequest;
import com.example.salonflow.ai.dto.chat.AiChatResponse;
import com.example.salonflow.ai.service.AiFacadeService;
import com.example.salonflow.ai.service.AiOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiFacadeServiceImpl implements AiFacadeService {

    private final AiOrchestratorService aiOrchestratorService;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        return aiOrchestratorService.execute(request);
    }
}
