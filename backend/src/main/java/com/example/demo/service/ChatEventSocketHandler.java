package com.example.demo.service;

import com.example.demo.domain.Chats;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Delivers chat events only to sessions authenticated as the chat's members. */
@Component
public class ChatEventSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatEventSocketHandler.class);
    private final ObjectMapper json;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public ChatEventSocketHandler(ObjectMapper objectMapper) {
        this.json = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        if (userId != null) {
            sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("Chat WebSocket connected for user {}", userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Chat WebSocket closed for user {} with {}", userId(session), status);
        remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        remove(session);
    }

    public void publish(long recipientUserId, String chatId, Chats.Message message) {
        String payload;
        try {
            payload = json.writeValueAsString(new Chats.Event("chat.message", chatId, message));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize chat event", exception);
        }
        Set<WebSocketSession> recipientSessions = sessions.getOrDefault(recipientUserId, Set.of());
        log.info("Publishing chat message {} in chat {} to user {} across {} WebSocket session(s)",
            message.id(), chatId, recipientUserId, recipientSessions.size());
        for (WebSocketSession session : recipientSessions) {
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
