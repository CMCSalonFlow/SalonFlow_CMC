package com.example.salonflow.ai.controller;

import com.example.salonflow.ai.dto.chat.AiChatMessage;
import com.example.salonflow.ai.dto.chat.AiChatRequest;
import com.example.salonflow.ai.dto.chat.AiChatResponse;
import com.example.salonflow.ai.service.AiFacadeService;
import com.example.salonflow.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class AiChatbotController {

    private final AiFacadeService aiFacadeService;

    @PostMapping("/message")
    public ResponseEntity<AiChatResponse> handleMessage(@RequestBody AiChatRequest request) {
        Long userId = getCurrentUserIdSafe();
        
        // Build request including security context userId if not explicitly passed
        AiChatRequest enrichedRequest = new AiChatRequest(
                request.conversationId(),
                userId != null ? userId : request.userId(),
                request.branchId(),
                request.useCase() != null ? request.useCase() : "chatbot",
                request.message(),
                request.history()
        );

        AiChatResponse response = aiFacadeService.chat(enrichedRequest);
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserIdSafe() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
                return ((CustomUserPrincipal) authentication.getPrincipal()).getId();
            }
        } catch (Exception ignored) {
            // Keep silent for anonymous requests
        }
        return null;
    }
}
