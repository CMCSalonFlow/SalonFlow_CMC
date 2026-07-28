package com.example.salonflow.ai.service;

import com.example.salonflow.ai.dto.chat.AiChatMessage;

import java.util.List;

public interface ConversationMemoryService {

    List<AiChatMessage> getConversationHistory(String conversationId);

    void saveUserMessage(String conversationId, AiChatMessage message);

    void saveAssistantMessage(String conversationId, AiChatMessage message);
}

