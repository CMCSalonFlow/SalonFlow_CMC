package com.example.salonflow.ai.prompt;

import com.example.salonflow.ai.dto.chat.AiChatRequest;

import java.util.List;

public interface AiPromptBuilder {

    String buildPrompt(AiChatRequest request, List<String> context);
}

