package com.example.demo.service;

import com.example.demo.domain.Chats;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Delivers chat events only to sessions authenticated as the chat's members. */
@Component
public class ChatEventSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper json;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public ChatEventSocketHandler(ObjectMapper objectMapper) {
        this.json = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        if (userId != null) sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        remove(session);
    }

    public void publish(long recipientUserId, String chatId, Chats.Message message) {
        String payload;
        try {
            payload = json.writeValueAsString(Map.of(
                "type", "chat.message",
                "chatId", chatId,
                "message", message.serialize()));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize chat event", exception);
        }
        for (WebSocketSession session : sessions.getOrDefault(recipientUserId, Set.of())) {
            try {
                synchronized (session) {
                    if (session.isOpen()) session.sendMessage(new TextMessage(payload));
                }
            } catch (Exception exception) {
                remove(session);
            }
        }
    }

    private Long userId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        return value instanceof Long userId ? userId : null;
    }

    private void remove(WebSocketSession session) {
        Long userId = userId(session);
        if (userId == null) return;
        sessions.computeIfPresent(userId, (ignored, userSessions) -> {
            userSessions.remove(session);
            return userSessions.isEmpty() ? null : userSessions;
        });
    }
}
