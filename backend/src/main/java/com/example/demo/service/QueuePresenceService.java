package com.example.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Maintains the short-lived online queue used by recommendations. */
@Service
public class QueuePresenceService {
    public static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    public static final Duration OFFLINE_AFTER = HEARTBEAT_INTERVAL.multipliedBy(3);
    public static final Duration SWIPE_IDLE_AFTER = Duration.ofMinutes(1);
    private static final String RECOMMENDATION_CACHE_PREFIX = "recommendations:";

    private final JdbcTemplate jdbcTemplate;
    private final ReadCache readCache;

    public record Status(boolean online, boolean looking, boolean permanent, String expiresAt) { }

    public QueuePresenceService(JdbcTemplate jdbcTemplate, ReadCache readCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.readCache = readCache;
    }

    public Status heartbeat(long userId) {
        removeExpiredPresences();
        Instant now = Instant.now();
        jdbcTemplate.update(
            "INSERT INTO user_site_presence(user_id,last_heartbeat_at) VALUES(?,?) "
                + "ON CONFLICT(user_id) DO UPDATE SET last_heartbeat_at=excluded.last_heartbeat_at",
            userId, now.toString());
        int refreshed = jdbcTemplate.update(
            "UPDATE user_queue_presence SET last_heartbeat_at=? WHERE user_id=?",
            now.toString(), userId);
        if (refreshed == 0) return new Status(true, false, false, now.plus(OFFLINE_AFTER).toString());
        return response(true, now, lastSwipeAt(userId));
    }

    public Status join(long userId) {
        Instant now = Instant.now();
        jdbcTemplate.update(
            "INSERT INTO user_queue_presence(user_id,last_heartbeat_at,last_swipe_at) VALUES(?,?,?) "
                + "ON CONFLICT(user_id) DO UPDATE SET last_heartbeat_at=excluded.last_heartbeat_at,last_swipe_at=excluded.last_swipe_at",
            userId, now.toString(), now.toString());
        invalidateRecommendationReads();
        return response(true, now, now);
    }

    public void recordSwipe(long userId) {
        Instant now = Instant.now();
        jdbcTemplate.update(
            "INSERT INTO user_queue_presence(user_id,last_heartbeat_at,last_swipe_at) VALUES(?,?,?) "
                + "ON CONFLICT(user_id) DO UPDATE SET last_heartbeat_at=excluded.last_heartbeat_at,last_swipe_at=excluded.last_swipe_at",
            userId, now.toString(), now.toString());
        invalidateRecommendationReads();
    }

    /** Test profiles are always available as local development recommendations. */
    public void joinPermanently(long userId) {
        jdbcTemplate.update("INSERT OR IGNORE INTO permanent_test_queue_users(user_id) VALUES(?)", userId);
        join(userId);
    }

    public void joinPermanently(List<Long> userIds) {
        if (userIds.isEmpty()) return;
        jdbcTemplate.batchUpdate("INSERT OR IGNORE INTO permanent_test_queue_users(user_id) VALUES(?)", userIds, 100,
            (statement, userId) -> statement.setLong(1, userId));
        invalidateRecommendationReads();
    }

    public void leave(long userId) {
        jdbcTemplate.update("DELETE FROM user_queue_presence WHERE user_id=?", userId);
        jdbcTemplate.update("DELETE FROM match_decisions WHERE actor_user_id=? AND decision='deferred'", userId);
        invalidateRecommendationReads();
    }

    public Status status(long userId) {
        removeExpiredPresences();
        jdbcTemplate.update("DELETE FROM user_site_presence WHERE last_heartbeat_at<=?", Instant.now().minus(OFFLINE_AFTER).toString());
        Boolean permanent = jdbcTemplate.query(
            "SELECT EXISTS(SELECT 1 FROM permanent_test_queue_users WHERE user_id=?)",
            resultSet -> {
                resultSet.next();
                return resultSet.getBoolean(1);
            }, userId);
        if (Boolean.TRUE.equals(permanent)) return new Status(true, true, true, null);
        QueueTimestamp queueTimestamp = jdbcTemplate.query(
            "SELECT last_heartbeat_at,last_swipe_at FROM user_queue_presence WHERE user_id=?",
            resultSet -> resultSet.next() ? new QueueTimestamp(Instant.parse(resultSet.getString(1)), Instant.parse(resultSet.getString(2))) : null, userId);
        if (queueTimestamp != null) return response(true, queueTimestamp.heartbeat(), queueTimestamp.swipe());
        String siteHeartbeat = jdbcTemplate.query(
            "SELECT last_heartbeat_at FROM user_site_presence WHERE user_id=?",
            resultSet -> resultSet.next() ? resultSet.getString(1) : null, userId);
        return siteHeartbeat == null
            ? new Status(false, false, false, null)
            : new Status(true, false, false, Instant.parse(siteHeartbeat).plus(OFFLINE_AFTER).toString());
    }

    public void removeExpiredPresences() {
        Instant now = Instant.now();
        int deleted = jdbcTemplate.update(
            "DELETE FROM user_queue_presence WHERE last_heartbeat_at<=? OR last_swipe_at<=?",
            now.minus(OFFLINE_AFTER).toString(), now.minus(SWIPE_IDLE_AFTER).toString());
        if (deleted > 0) invalidateRecommendationReads();
    }

    private void invalidateRecommendationReads() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            readCache.invalidatePrefix(RECOMMENDATION_CACHE_PREFIX);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                readCache.invalidatePrefix(RECOMMENDATION_CACHE_PREFIX);
            }
        });
    }

    private Instant lastSwipeAt(long userId) {
        return jdbcTemplate.query(
            "SELECT last_swipe_at FROM user_queue_presence WHERE user_id=?",
            resultSet -> resultSet.next() ? Instant.parse(resultSet.getString(1)) : null, userId);
    }

    private Status response(boolean online, Instant heartbeat, Instant swipe) {
        Instant expiresAt = heartbeat.plus(OFFLINE_AFTER);
        if (swipe != null && swipe.plus(SWIPE_IDLE_AFTER).isBefore(expiresAt)) expiresAt = swipe.plus(SWIPE_IDLE_AFTER);
        return new Status(online, online, false, expiresAt.toString());
    }

    private record QueueTimestamp(Instant heartbeat, Instant swipe) { }
}
