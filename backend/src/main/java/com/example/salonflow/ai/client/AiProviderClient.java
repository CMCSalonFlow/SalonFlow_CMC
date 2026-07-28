package com.example.salonflow.ai.client;

import com.example.salonflow.ai.dto.chat.AiChatRequest;
import com.example.salonflow.ai.dto.chat.AiChatResponse;

public interface AiProviderClient {

    AiChatResponse chat(AiChatRequest request);
}

