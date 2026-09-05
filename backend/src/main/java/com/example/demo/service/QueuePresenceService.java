package com.example.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Maintains the short-lived online queue used by recommendations. */
@Service
public class QueuePresenceService {
    public static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    public static final Duration OFFLINE_AFTER = HEARTBEAT_INTERVAL.multipliedBy(3);
    private static final String RECOMMENDATION_CACHE_PREFIX = "recommendations:";

    private final JdbcTemplate jdbcTemplate;
    private final ReadCache readCache;

    public QueuePresenceService(JdbcTemplate jdbcTemplate, ReadCache readCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.readCache = readCache;
    }

    public Map<String, Object> heartbeat(long userId) {
        return join(userId);
    }

    public Map<String, Object> join(long userId) {
        Instant now = Instant.now();
        jdbcTemplate.update(
            "INSERT INTO user_queue_presence(user_id,last_heartbeat_at) VALUES(?,?) "
                + "ON CONFLICT(user_id) DO UPDATE SET last_heartbeat_at=excluded.last_heartbeat_at",
            userId, now.toString());
        readCache.invalidatePrefix(RECOMMENDATION_CACHE_PREFIX);
        return response(true, now);
    }

    /** Test profiles are always available as local development recommendations. */
    public void joinPermanently(long userId) {
        jdbcTemplate.update("INSERT OR IGNORE INTO permanent_test_queue_users(user_id) VALUES(?)", userId);
        join(userId);
    }

    public void leave(long userId) {
        jdbcTemplate.update("DELETE FROM user_queue_presence WHERE user_id=?", userId);
        readCache.invalidatePrefix(RECOMMENDATION_CACHE_PREFIX);
    }

    public Map<String, Object> status(long userId) {
        removeExpiredPresences();
        Boolean permanent = jdbcTemplate.query(
            "SELECT EXISTS(SELECT 1 FROM permanent_test_queue_users WHERE user_id=?)",
            resultSet -> {
                resultSet.next();
                return resultSet.getBoolean(1);
            }, userId);
        if (Boolean.TRUE.equals(permanent)) return Map.of("online", true, "permanent", true);
        String heartbeat = jdbcTemplate.query(
            "SELECT last_heartbeat_at FROM user_queue_presence WHERE user_id=?",
            resultSet -> resultSet.next() ? resultSet.getString(1) : null, userId);
        return heartbeat == null ? Map.of("online", false) : response(true, Instant.parse(heartbeat));
    }

    public void removeExpiredPresences() {
        int deleted = jdbcTemplate.update("DELETE FROM user_queue_presence WHERE last_heartbeat_at<=?", Instant.now().minus(OFFLINE_AFTER).toString());
        if (deleted > 0) readCache.invalidatePrefix(RECOMMENDATION_CACHE_PREFIX);
    }

    private Map<String, Object> response(boolean online, Instant heartbeat) {
        return Map.of("online", online, "expiresAt", heartbeat.plus(OFFLINE_AFTER).toString());
    }
}
