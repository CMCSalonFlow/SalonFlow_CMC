package com.example.salonflow.ai.memory;

import com.example.salonflow.ai.dto.chat.AiChatMessage;

import java.time.Duration;
import java.util.List;

public interface AiConversationStore {

    List<AiChatMessage> getHistory(String conversationId);

    void append(String conversationId, AiChatMessage message, Duration ttl);
}

