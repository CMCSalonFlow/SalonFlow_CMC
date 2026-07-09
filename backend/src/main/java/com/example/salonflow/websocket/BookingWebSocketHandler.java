package com.example.salonflow.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class BookingWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connection established. Session ID: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket connection closed. Session ID: {}, Status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("Received message: {} from session: {}", message.getPayload(), session.getId());
        // Currently read-only channel for notifications, no client-to-server messaging expected.
    }

    public void broadcastBookingUpdate(Long branchId, Long staffId, String date) {
        String payload = String.format(
                "{\"type\":\"BOOKING_UPDATE\",\"branchId\":%d,\"staffId\":%s,\"date\":\"%s\"}",
                branchId,
                staffId != null ? staffId.toString() : "null",
                date
        );
        log.info("Broadcasting booking update: {}", payload);
        TextMessage textMessage = new TextMessage(payload);

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Failed to send WebSocket message to session: {}", session.getId(), e);
                }
            } else {
                sessions.remove(session);
            }
        }
    }
}
