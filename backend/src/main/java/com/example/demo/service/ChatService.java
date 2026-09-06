package com.example.demo.service;

import com.example.demo.api.ApiException;
import com.example.demo.domain.Chats;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private static final Duration CHAT_TTL = Duration.ofDays(1);
    private static final Duration CHAT_LIST_CACHE_TTL = Duration.ofMinutes(1);
    private static final Duration CHAT_DETAIL_CACHE_TTL = Duration.ofSeconds(30);
    private static final String CHAT_LIST_CACHE_PREFIX = "chats:";
    private static final String CHAT_DETAIL_CACHE_PREFIX = "chat:";

    private final JdbcTemplate jdbcTemplate;
    private final ReadCache readCache;
    private final QueuePresenceService queuePresence;

    public ChatService(JdbcTemplate jdbcTemplate, ReadCache readCache, QueuePresenceService queuePresence) {
        this.jdbcTemplate = jdbcTemplate;
        this.readCache = readCache;
        this.queuePresence = queuePresence;
    }

    public void member(long userId, String chatId) {
        removeExpiredChats();
        Boolean isMember = jdbcTemplate.query(
            "SELECT EXISTS(SELECT 1 FROM chat_members WHERE chat_id=? AND user_id=?)",
            resultSet -> {
                resultSet.next();
                return resultSet.getBoolean(1);
            }, chatId, userId);
        if (!Boolean.TRUE.equals(isMember)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "not_chat_member", "You are not a member of this chat");
        }
    }

    public List<Chats.Summary> chats(long userId) {
        removeExpiredChats();
        ReadCache.Key<List<Chats.Summary>> cacheKey = ReadCache.Key.of(CHAT_LIST_CACHE_PREFIX + userId);
        return readCache.getOrLoad(cacheKey, CHAT_LIST_CACHE_TTL, () -> loadChatSummaries(userId));
    }

    public Map<String, Object> chat(long userId, String chatId, String cursor) {
        member(userId, chatId);
        ReadCache.Key<Map<String, Object>> cacheKey = ReadCache.Key.of(chatCacheKey(userId, chatId, cursor));
        return readCache.getOrLoad(cacheKey, CHAT_DETAIL_CACHE_TTL, () -> loadChat(chatId, cursor));
    }

    public Map<String, Object> message(long userId, String chatId, String body) {
        member(userId, chatId);
        String normalizedBody = validateMessageBody(body);
        String createdAt = Instant.now().toString();
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO messages(chat_id,sender_user_id,body,created_at) VALUES(?,?,?,?)",
                new String[]{"id"});
            statement.setString(1, chatId);
            statement.setLong(2, userId);
            statement.setString(3, normalizedBody);
            statement.setString(4, createdAt);
            return statement;
        }, generatedKeyHolder);

        Number generatedMessageId = generatedKeyHolder.getKey();
        if (generatedMessageId == null) {
            throw new IllegalStateException("Created message did not return an ID");
        }
        invalidateChatReads();
        Map<String, Object> response = new LinkedHashMap<>(
            new Chats.Message(generatedMessageId.longValue(), userId, normalizedBody, createdAt).serialize());
        response.put("chatId", chatId);
        return response;
    }

    /** Ends a direct chat and removes its corresponding match without changing queue presence. */
    @Transactional
    public void unmatch(long userId, String chatId) {
        member(userId, chatId);
        Long otherUserId = jdbcTemplate.query(
            "SELECT user_id FROM chat_members WHERE chat_id=? AND user_id<>?",
            resultSet -> resultSet.next() ? resultSet.getLong(1) : null, chatId, userId);
        if (otherUserId == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "chat_not_found", "Chat not found");
        }
        long firstUserId = Math.min(userId, otherUserId);
        long secondUserId = Math.max(userId, otherUserId);
        jdbcTemplate.update("DELETE FROM matches WHERE user_a_id=? AND user_b_id=?", firstUserId, secondUserId);
        jdbcTemplate.update("DELETE FROM match_decisions WHERE (actor_user_id=? AND target_user_id=?) OR (actor_user_id=? AND target_user_id=?)",
            userId, otherUserId, otherUserId, userId);
        jdbcTemplate.update("DELETE FROM chats WHERE id=?", chatId);
        queuePresence.join(userId);
        queuePresence.join(otherUserId);
        invalidateChatReads();
        readCache.invalidatePrefix("matches:");
    }

    private List<Chats.Summary> loadChatSummaries(long userId) {
        return jdbcTemplate.query(
            "SELECT c.id,(SELECT other_member.user_id FROM chat_members other_member WHERE other_member.chat_id=c.id AND other_member.user_id<>? LIMIT 1),"
                + "(SELECT u.username FROM chat_members other_member JOIN users u ON u.id=other_member.user_id WHERE other_member.chat_id=c.id AND other_member.user_id<>? LIMIT 1),c.created_at,(SELECT body FROM messages m WHERE m.chat_id=c.id "
                + "ORDER BY created_at DESC,id DESC LIMIT 1) latest "
                + "FROM chats c JOIN chat_members cm ON cm.chat_id=c.id "
                + "WHERE cm.user_id=? ORDER BY c.created_at DESC",
            resultSet -> {
                List<Chats.Summary> summaries = new ArrayList<>();
                while (resultSet.next()) {
                    summaries.add(new Chats.Summary(
                        resultSet.getString(1), resultSet.getLong(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5)));
                }
                return List.copyOf(summaries);
            }, userId, userId, userId);
    }

    private Map<String, Object> loadChat(String chatId, String cursor) {
        ChatPageQuery pageQuery = ChatPageQuery.from(chatId, cursor);
        List<Chats.Message> messages = jdbcTemplate.query(pageQuery.sql(), resultSet -> {
            List<Chats.Message> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new Chats.Message(
                    resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3), resultSet.getString(4)));
            }
            return result;
        }, pageQuery.parameters());

        String nextCursor = messages.size() > 50
            ? messages.get(49).createdAt() + "," + messages.get(49).id()
            : null;
        List<Chats.Message> pageMessages = messages.size() > 50 ? List.copyOf(messages.subList(0, 50)) : messages;
        return Chats.serializeDetail(chatId, pageMessages, nextCursor);
    }

    private String validateMessageBody(String body) {
        if (body == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_message", "message must be 1-2000 characters");
        }
        String normalizedBody = body.trim();
        if (normalizedBody.isEmpty() || normalizedBody.length() > 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_message", "message must be 1-2000 characters");
        }
        return normalizedBody;
    }

    private String chatCacheKey(long userId, String chatId, String cursor) {
        return CHAT_DETAIL_CACHE_PREFIX + userId + ":" + chatId + ":" + (cursor == null ? "first" : cursor);
    }

    private void invalidateChatReads() {
        readCache.invalidatePrefix(CHAT_LIST_CACHE_PREFIX);
        readCache.invalidatePrefix(CHAT_DETAIL_CACHE_PREFIX);
    }

    /** Removes expired chat sessions and releases their users back into matching. */
    public void removeExpiredChats() {
        String expirationThreshold = Instant.now().minus(CHAT_TTL).toString();
        int deletedMatches = jdbcTemplate.update(
            "DELETE FROM matches WHERE EXISTS (SELECT 1 FROM chats c "
                + "JOIN chat_members first_member ON first_member.chat_id=c.id "
                + "JOIN chat_members second_member ON second_member.chat_id=c.id "
                + "WHERE c.created_at<=? AND first_member.user_id=matches.user_a_id "
                + "AND second_member.user_id=matches.user_b_id)",
            expirationThreshold);
        int deletedChats = jdbcTemplate.update("DELETE FROM chats WHERE created_at<=?", expirationThreshold);
        if (deletedMatches > 0 || deletedChats > 0) {
            invalidateChatReads();
            readCache.invalidatePrefix("recommendations:");
            readCache.invalidatePrefix("matches:");
        }
    }

    public boolean hasActiveChat(long userId) {
        removeExpiredChats();
        Boolean activeChat = jdbcTemplate.query(
            "SELECT EXISTS(SELECT 1 FROM chats c JOIN chat_members cm ON cm.chat_id=c.id "
                + "WHERE cm.user_id=? AND c.created_at>?)",
            resultSet -> {
                resultSet.next();
                return resultSet.getBoolean(1);
            }, userId, Instant.now().minus(CHAT_TTL).toString());
        return Boolean.TRUE.equals(activeChat);
    }

    private record ChatPageQuery(String sql, Object[] parameters) {
        private static ChatPageQuery from(String chatId, String cursor) {
            String sql = "SELECT id,sender_user_id,body,created_at FROM messages WHERE chat_id=? ";
            if (cursor == null) {
                return new ChatPageQuery(sql + "ORDER BY created_at DESC,id DESC LIMIT 51", new Object[]{chatId});
            }

            int separatorIndex = cursor.lastIndexOf(',');
            if (separatorIndex < 1) {
                throw invalidCursor();
            }
            try {
                long messageId = Long.parseLong(cursor.substring(separatorIndex + 1));
                return new ChatPageQuery(sql + "AND (created_at,id) < (?,?) ORDER BY created_at DESC,id DESC LIMIT 51",
                    new Object[]{chatId, cursor.substring(0, separatorIndex), messageId});
            } catch (NumberFormatException exception) {
                throw invalidCursor();
            }
        }

        private static ApiException invalidCursor() {
            return new ApiException(HttpStatus.BAD_REQUEST, "invalid_cursor", "cursor must be createdAt,id");
        }
    }
}
