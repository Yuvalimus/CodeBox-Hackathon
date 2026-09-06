package com.example.demo.service;

import com.example.demo.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchService {
    static final Duration DECISION_TTL = Duration.ofMinutes(5);
    private static final String RECOMMENDATION_CACHE_PREFIX = "recommendations:";
    private static final String MATCH_CACHE_PREFIX = "matches:";
    private static final String CHAT_CACHE_PREFIX = "chats:";
    private static final String CHAT_DETAIL_CACHE_PREFIX = "chat:";

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final ChatService chats;
    private final ReadCache readCache;

    public MatchService(JdbcTemplate jdbcTemplate, UserService userService, ChatService chatService, ReadCache readCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.chats = chatService;
        this.readCache = readCache;
    }

    @Transactional
    public Map<String, Object> accept(long currentUserId, long targetUserId) {
        validateTarget(currentUserId, targetUserId);
        String createdAt = Instant.now().toString();
        jdbcTemplate.update(
            "INSERT INTO match_decisions(actor_user_id,target_user_id,decision,created_at) "
                + "VALUES(?,?, 'accepted',?) ON CONFLICT(actor_user_id,target_user_id) "
                + "DO UPDATE SET decision='accepted',created_at=excluded.created_at",
            currentUserId, targetUserId, createdAt);

        if (!hasReciprocalAcceptance(currentUserId, targetUserId)) {
            invalidateRelationshipReads();
            return Map.of("decision", "accepted", "matched", false);
        }

        long firstUserId = Math.min(currentUserId, targetUserId);
        long secondUserId = Math.max(currentUserId, targetUserId);
        jdbcTemplate.update("INSERT OR IGNORE INTO matches(user_a_id,user_b_id,created_at) VALUES(?,?,?)",
            firstUserId, secondUserId, createdAt);
        Long matchId = jdbcTemplate.queryForObject("SELECT id FROM matches WHERE user_a_id=? AND user_b_id=?",
            Long.class, firstUserId, secondUserId);
        if (matchId == null) {
            throw new IllegalStateException("Mutual match could not be loaded");
        }

        String chatId = findOrCreateDirectChat(currentUserId, targetUserId, createdAt);
        invalidateRelationshipReads();
        return Map.of("decision", "accepted", "matched", true, "match", Map.of("id", matchId), "chat", Map.of("id", chatId));
    }

    public void reject(long currentUserId, long targetUserId) {
        validateTarget(currentUserId, targetUserId);
        jdbcTemplate.update(
            "INSERT INTO match_decisions(actor_user_id,target_user_id,decision,created_at) VALUES(?,?,'rejected',?) "
                + "ON CONFLICT(actor_user_id,target_user_id) DO UPDATE SET decision='rejected',created_at=excluded.created_at",
            currentUserId, targetUserId, Instant.now().toString());
        invalidateRelationshipReads();
    }

    private boolean hasReciprocalAcceptance(long currentUserId, long targetUserId) {
        return jdbcTemplate.query(
            "SELECT EXISTS(SELECT 1 FROM match_decisions WHERE actor_user_id=? AND target_user_id=? AND decision='accepted' AND created_at>?)",
            resultSet -> {
                resultSet.next();
                return resultSet.getBoolean(1);
            }, targetUserId, currentUserId, Instant.now().minus(DECISION_TTL).toString());
    }

    private String findOrCreateDirectChat(long currentUserId, long targetUserId, String createdAt) {
        String existingChatId = jdbcTemplate.query(
            "SELECT c.id FROM chats c JOIN chat_members first_member ON first_member.chat_id=c.id "
                + "JOIN chat_members second_member ON second_member.chat_id=c.id "
                + "WHERE first_member.user_id=? AND second_member.user_id=? GROUP BY c.id HAVING COUNT(*)=2",
            resultSet -> resultSet.next() ? resultSet.getString(1) : null, currentUserId, targetUserId);
        if (existingChatId != null) {
            return existingChatId;
        }

        String chatId = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO chats(id,created_at) VALUES(?,?)", chatId, createdAt);
        jdbcTemplate.update("INSERT INTO chat_members(chat_id,user_id,joined_at) VALUES(?,?,?),(?,?,?)",
            chatId, currentUserId, createdAt, chatId, targetUserId, createdAt);
        return chatId;
    }

    private void validateTarget(long currentUserId, long targetUserId) {
        if (currentUserId == targetUserId) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_target", "You cannot choose yourself");
        }
        if (!userService.exists(targetUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "User not found");
        }
        if (chats.hasActiveChat(currentUserId) || chats.hasActiveChat(targetUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "user_unavailable", "Users with an active match cannot be matched again");
        }
    }

    private void invalidateRelationshipReads() {
        readCache.invalidatePrefix(RECOMMENDATION_CACHE_PREFIX);
        readCache.invalidatePrefix(MATCH_CACHE_PREFIX);
        readCache.invalidatePrefix(CHAT_CACHE_PREFIX);
        readCache.invalidatePrefix(CHAT_DETAIL_CACHE_PREFIX);
    }
}
