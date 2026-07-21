package com.example.salonflow.ai.dto.chat;

import java.util.List;

public record AiChatRequest(
        String conversationId,
        Long userId,
        Long branchId,
        String useCase,
        String message,
        List<AiChatMessage> history
) {
}

