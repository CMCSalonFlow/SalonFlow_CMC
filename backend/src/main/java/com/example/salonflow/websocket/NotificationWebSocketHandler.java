package com.example.salonflow.websocket;

import com.example.salonflow.dto.notification.NotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        Long userId = extractUserId(session);
        if (userId != null) {
            session.getAttributes().put("userId", userId);
        }
        log.info("Notification WebSocket connected. Session ID: {}, UserId: {}", session.getId(), userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Notification WebSocket closed. Session ID: {}, Status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            if (payload != null && payload.contains("AUTH")) {
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                if (data.containsKey("userId")) {
                    Long userId = Long.valueOf(data.get("userId").toString());
                    session.getAttributes().put("userId", userId);
                    log.info("Session {} authenticated with userId {}", session.getId(), userId);
                }
            }
        } catch (Exception e) {
            log.debug("Handled non-auth WS text message: {}", e.getMessage());
        }
    }

    public void sendNotificationToUser(Long recipientId, NotificationResponse notification, long unreadCount) {
        try {
            Map<String, Object> eventMap = Map.of(
                    "type", "NEW_NOTIFICATION",
                    "userId", recipientId,
                    "unreadCount", unreadCount,
                    "notification", notification
            );
            String jsonPayload = objectMapper.writeValueAsString(eventMap);
            TextMessage textMessage = new TextMessage(jsonPayload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    Object sUserIdObj = session.getAttributes().get("userId");
                    Long sUserId = sUserIdObj != null ? Long.valueOf(sUserIdObj.toString()) : null;

                    // Send if target matches user or session has no specific user bound
                    if (sUserId == null || sUserId.equals(recipientId)) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            log.error("Failed to send notification WS message to session: {}", session.getId(), e);
                        }
                    }
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            log.error("Error serializing notification WS payload", e);
        }
    }

    private Long extractUserId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                for (String param : uri.getQuery().split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "userId".equalsIgnoreCase(pair[0])) {
                        return Long.valueOf(pair[1]);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract userId from query: {}", e.getMessage());
        }
        return null;
    }
}
