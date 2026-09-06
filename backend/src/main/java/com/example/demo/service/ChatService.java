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
import java.util.List;

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
    private final UserService users;
    private final ChatEventSocketHandler events;

    public ChatService(JdbcTemplate jdbcTemplate, ReadCache readCache, QueuePresenceService queuePresence, UserService userService,
                       ChatEventSocketHandler chatEventSocketHandler) {
        this.jdbcTemplate = jdbcTemplate;
        this.readCache = readCache;
        this.queuePresence = queuePresence;
        this.users = userService;
        this.events = chatEventSocketHandler;
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

    public Chats.Detail chat(long userId, String chatId, String cursor) {
        member(userId, chatId);
        ReadCache.Key<Chats.Detail> cacheKey = ReadCache.Key.of(chatCacheKey(userId, chatId, cursor));
        return readCache.getOrLoad(cacheKey, CHAT_DETAIL_CACHE_TTL, () -> loadChat(userId, chatId, cursor));
    }

    public Chats.PostedMessage message(long userId, String chatId, String body) {
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
        Chats.Message eventMessage = new Chats.Message(generatedMessageId.longValue(), userId, normalizedBody, createdAt);
        for (Long memberId : jdbcTemplate.queryForList("SELECT user_id FROM chat_members WHERE chat_id=?", Long.class, chatId)) {
            events.publish(memberId, chatId, eventMessage);
        }
        return new Chats.PostedMessage(eventMessage.id(), chatId, eventMessage.senderUserId(), eventMessage.body(), eventMessage.createdAt());
    }

    /** Ends a direct chat, requeues both users, and starts a short rematch cooldown. */
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
        String cooldownStartedAt = Instant.now().toString();
        jdbcTemplate.update(
            "INSERT INTO match_decisions(actor_user_id,target_user_id,decision,created_at) VALUES(?,?,'rejected',?) "
                + "ON CONFLICT(actor_user_id,target_user_id) DO UPDATE SET decision='rejected',created_at=excluded.created_at",
            userId, otherUserId, cooldownStartedAt);
        jdbcTemplate.update(
            "INSERT INTO match_decisions(actor_user_id,target_user_id,decision,created_at) VALUES(?,?,'rejected',?) "
                + "ON CONFLICT(actor_user_id,target_user_id) DO UPDATE SET decision='rejected',created_at=excluded.created_at",
            otherUserId, userId, cooldownStartedAt);
        jdbcTemplate.update("DELETE FROM chats WHERE id=?", chatId);
        queuePresence.join(userId);
        queuePresence.join(otherUserId);
        invalidateChatReads();
        readCache.invalidatePrefix("matches:");
    }

    private List<Chats.Summary> loadChatSummaries(long userId) {
        return jdbcTemplate.query(
            "SELECT c.id,(SELECT other_member.user_id FROM chat_members other_member WHERE other_member.chat_id=c.id AND other_member.user_id<>? LIMIT 1),"
                + "(SELECT u.username FROM chat_members other_member JOIN users u ON u.id=other_member.user_id WHERE other_member.chat_id=c.id AND other_member.user_id<>? LIMIT 1),"
                + "(SELECT u.picture_url FROM chat_members other_member JOIN users u ON u.id=other_member.user_id WHERE other_member.chat_id=c.id AND other_member.user_id<>? LIMIT 1),"
                + "(SELECT u.avatar FROM chat_members other_member JOIN users u ON u.id=other_member.user_id WHERE other_member.chat_id=c.id AND other_member.user_id<>? LIMIT 1),c.created_at,(SELECT body FROM messages m WHERE m.chat_id=c.id "
                + "ORDER BY created_at DESC,id DESC LIMIT 1) latest "
                + "FROM chats c JOIN chat_members cm ON cm.chat_id=c.id "
                + "WHERE cm.user_id=? ORDER BY c.created_at DESC",
            resultSet -> {
                List<Chats.Summary> summaries = new ArrayList<>();
                while (resultSet.next()) {
                    summaries.add(new Chats.Summary(
                        resultSet.getString(1), resultSet.getLong(2), resultSet.getString(3), resultSet.getString(4),
                        resultSet.getString(5), resultSet.getString(6), resultSet.getString(7)));
                }
                return List.copyOf(summaries);
            }, userId, userId, userId, userId, userId);
    }

    private Chats.Detail loadChat(long userId, String chatId, String cursor) {
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
        Long buddyId = jdbcTemplate.query(
            "SELECT user_id FROM chat_members WHERE chat_id=? AND user_id<>?",
            resultSet -> resultSet.next() ? resultSet.getLong(1) : null, chatId, userId);
        if (buddyId == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "chat_not_found", "Chat not found");
        }
        return new Chats.Detail(chatId, pageMessages, nextCursor, users.find(buddyId).publicProfile());
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
