package com.example.demo.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API representations for chat rows; controllers never build chat JSON themselves.
 */
public final class Chats {
    private Chats() {
    }

    public static Map<String, Object> serializeDetail(long id, List<Message> messages, String nextCursor) {
        return new Detail(id, messages, nextCursor).toMap();
    }

    public record Detail(long id, List<Message> messages, String nextCursor) {
        public Detail {
            messages = List.copyOf(messages);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", id);
            out.put("messages", messages.stream().map(Message::serialize).toList());
            out.put("nextCursor", nextCursor);
            return out;
        }
    }

    public record Summary(long id, String createdAt, String latestMessage) {
        public Map<String, Object> serialize() {
            return Map.of("id", id, "createdAt", createdAt, "latestMessage", latestMessage == null ? "" : latestMessage);
        }
    }

    public record Message(long id, long senderUserId, String body, String createdAt) {
        public Map<String, Object> serialize() {
            return Map.of("id", id, "senderUserId", senderUserId, "body", body, "createdAt", createdAt);
        }
    }
}
