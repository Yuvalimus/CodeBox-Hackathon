package com.example.demo.service;

import com.example.demo.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
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
    private final QueuePresenceService queuePresence;
    private final ReadCache readCache;
    private final TestMatchPolicy testMatches;

    public record MatchReference(long id) { }
    public record ChatReference(String id) { }
    public record Decision(String decision, boolean matched, MatchReference match, ChatReference chat) { }

    public MatchService(JdbcTemplate jdbcTemplate, UserService userService, QueuePresenceService queuePresenceService, ReadCache readCache, TestMatchPolicy testMatches) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.queuePresence = queuePresenceService;
        this.readCache = readCache;
        this.testMatches = testMatches;
    }

    @Transactional
    public Optional<Decision> accept(long currentUserId, long targetUserId) {
        validateTarget(currentUserId, targetUserId);
        String createdAt = Instant.now().toString();
        queuePresence.recordSwipe(currentUserId);
        if (hasActiveCooldown(currentUserId, targetUserId)) {
            return Optional.empty();
        }
        recordDecision(currentUserId, targetUserId, "accepted", createdAt);

        boolean reciprocal = hasReciprocalAcceptance(currentUserId, targetUserId);
        if (!reciprocal && testMatches.accepts(targetUserId)) {
            recordDecision(targetUserId, currentUserId, "accepted", createdAt);
            reciprocal = true;
        }
        if (!reciprocal) {
            invalidateRelationshipReads();
            return Optional.of(new Decision("accepted", false, null, null));
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
        clearOutstandingRequests(currentUserId, targetUserId);
        invalidateRelationshipReads();
        return Optional.of(new Decision("accepted", true, new MatchReference(matchId), new ChatReference(chatId)));
    }

    @Transactional
    public void reject(long currentUserId, long targetUserId) {
        validateTarget(currentUserId, targetUserId);
        queuePresence.recordSwipe(currentUserId);
        recordCooldown(currentUserId, targetUserId, Instant.now().toString());
        invalidateRelationshipReads();
    }

    private void recordCooldown(long firstUserId, long secondUserId, String createdAt) {
        recordDecision(firstUserId, secondUserId, "rejected", createdAt);
        recordDecision(secondUserId, firstUserId, "rejected", createdAt);
    }

    private void recordDecision(long actorUserId, long targetUserId, String decision, String createdAt) {
        jdbcTemplate.update(
            "INSERT INTO match_decisions(actor_user_id,target_user_id,decision,created_at) VALUES(?,?,?,?) "
                + "ON CONFLICT(actor_user_id,target_user_id) DO UPDATE SET decision=excluded.decision,created_at=excluded.created_at",
            actorUserId, targetUserId, decision, createdAt);
    }

    private void clearOutstandingRequests(long firstUserId, long secondUserId) {
        jdbcTemplate.update(
            "DELETE FROM match_decisions WHERE actor_user_id IN (?,?) AND target_user_id NOT IN (?,?) "
                + "AND decision IN ('accepted','deferred')",
            firstUserId, secondUserId, firstUserId, secondUserId);
    }

    private boolean hasReciprocalAcceptance(long currentUserId, long targetUserId) {
        return jdbcTemplate.query(
            "SELECT EXISTS(SELECT 1 FROM match_decisions WHERE actor_user_id=? AND target_user_id=? "
                + "AND (decision='deferred' OR (decision='accepted' AND created_at>?)))",
            resultSet -> {
                resultSet.next();
                return resultSet.getBoolean(1);
            }, targetUserId, currentUserId, Instant.now().minus(DECISION_TTL).toString());
    }

    private boolean hasActiveCooldown(long firstUserId, long secondUserId) {
        return jdbcTemplate.query(
            "SELECT EXISTS(SELECT 1 FROM match_decisions WHERE decision='rejected' AND created_at>? "
                + "AND ((actor_user_id=? AND target_user_id=?) OR (actor_user_id=? AND target_user_id=?)))",
            resultSet -> {
                resultSet.next();
                return resultSet.getBoolean(1);
            }, Instant.now().minus(DECISION_TTL).toString(), firstUserId, secondUserId, secondUserId, firstUserId);
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
    }

    private void invalidateRelationshipReads() {
        readCache.invalidatePrefix(RECOMMENDATION_CACHE_PREFIX);
        readCache.invalidatePrefix(MATCH_CACHE_PREFIX);
        readCache.invalidatePrefix(CHAT_CACHE_PREFIX);
        readCache.invalidatePrefix(CHAT_DETAIL_CACHE_PREFIX);
    }
}
