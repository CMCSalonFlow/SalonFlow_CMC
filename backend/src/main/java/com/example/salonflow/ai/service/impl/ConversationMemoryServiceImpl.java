package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.chat.AiChatMessage;
import com.example.salonflow.ai.memory.AiConversationStore;
import com.example.salonflow.ai.service.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    private final AiConversationStore conversationStore;
    private final AiProperties aiProperties;

    private Duration getTtl() {
        return Duration.ofMinutes(aiProperties.getConversationTtlMinutes() != null 
                ? aiProperties.getConversationTtlMinutes() 
                : 30);
    }

    @Override
    public List<AiChatMessage> getConversationHistory(String conversationId) {
        return conversationStore.getHistory(conversationId);
    }

    @Override
    public void saveUserMessage(String conversationId, AiChatMessage message) {
        conversationStore.append(conversationId, message, getTtl());
    }

    @Override
    public void saveAssistantMessage(String conversationId, AiChatMessage message) {
        conversationStore.append(conversationId, message, getTtl());
    }
}
