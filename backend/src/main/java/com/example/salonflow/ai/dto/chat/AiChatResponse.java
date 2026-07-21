package com.example.salonflow.ai.dto.chat;

import java.util.List;

public record AiChatResponse(
        String conversationId,
        String answer,
        List<String> sources,
        boolean needsHumanHandoff
) {
}

