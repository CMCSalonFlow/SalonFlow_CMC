package com.example.salonflow.ai.memory;

import com.example.salonflow.ai.dto.chat.AiChatMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiConversationStoreImpl implements AiConversationStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private String getRedisKey(String conversationId) {
        return "session:chatbot:history:" + conversationId;
    }

    @Override
    public List<AiChatMessage> getHistory(String conversationId) {
        String key = getRedisKey(conversationId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<AiChatMessage>>() {});
        } catch (Exception e) {
            log.error("Failed to read chat history from Redis for conversationId: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void append(String conversationId, AiChatMessage message, Duration ttl) {
        String key = getRedisKey(conversationId);
        try {
            List<AiChatMessage> history = getHistory(conversationId);
            history.add(message);
            String json = objectMapper.writeValueAsString(history);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.error("Failed to append chat message to Redis for conversationId: {}", conversationId, e);
        }
    }
}
