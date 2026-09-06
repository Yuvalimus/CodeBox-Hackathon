package com.example.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;

/** Maintains the short-lived online queue used by recommendations. */
@Service
public class QueuePresenceService {
    public static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    public static final Duration OFFLINE_AFTER = HEARTBEAT_INTERVAL.multipliedBy(3);
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
        return response(true, now);
    }

    public Status join(long userId) {
        Instant now = Instant.now();
        jdbcTemplate.update(
            "INSERT INTO user_queue_presence(user_id,last_heartbeat_at) VALUES(?,?) "
                + "ON CONFLICT(user_id) DO UPDATE SET last_heartbeat_at=excluded.last_heartbeat_at",
            userId, now.toString());
        invalidateRecommendationReads();
        return response(true, now);
    }

    /** Test profiles are always available as local development recommendations. */
    public void joinPermanently(long userId) {
        jdbcTemplate.update("INSERT OR IGNORE INTO permanent_test_queue_users(user_id) VALUES(?)", userId);
        join(userId);
    }

    public void leave(long userId) {
        jdbcTemplate.update("DELETE FROM user_queue_presence WHERE user_id=?", userId);
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
        String heartbeat = jdbcTemplate.query(
            "SELECT last_heartbeat_at FROM user_queue_presence WHERE user_id=?",
            resultSet -> resultSet.next() ? resultSet.getString(1) : null, userId);
        if (heartbeat != null) return response(true, Instant.parse(heartbeat));
        String siteHeartbeat = jdbcTemplate.query(
            "SELECT last_heartbeat_at FROM user_site_presence WHERE user_id=?",
            resultSet -> resultSet.next() ? resultSet.getString(1) : null, userId);
        return siteHeartbeat == null
            ? new Status(false, false, false, null)
            : new Status(true, false, false, Instant.parse(siteHeartbeat).plus(OFFLINE_AFTER).toString());
    }

    public void removeExpiredPresences() {
        int deleted = jdbcTemplate.update("DELETE FROM user_queue_presence WHERE last_heartbeat_at<=?", Instant.now().minus(OFFLINE_AFTER).toString());
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

    private Status response(boolean online, Instant heartbeat) {
        return new Status(online, online, false, heartbeat.plus(OFFLINE_AFTER).toString());
    }
}
