package com.example.demo.domain;

import java.util.List;

/**
 * API representations for chat rows; controllers never build chat JSON themselves.
 */
public final class Chats {
    private Chats() {
    }

    public record Detail(String id, List<Message> messages, String nextCursor, Users.PublicProfile buddy) {
        public Detail {
            messages = List.copyOf(messages);
        }
    }

    public record Summary(String id, long userId, String username, String createdAt, String latestMessage) {
        public Summary {
            latestMessage = latestMessage == null ? "" : latestMessage;
        }
    }

    public record Message(long id, long senderUserId, String body, String createdAt) { }

    public record PostedMessage(long id, String chatId, long senderUserId, String body, String createdAt) { }

    public record Event(String type, String chatId, Message message) { }
}
