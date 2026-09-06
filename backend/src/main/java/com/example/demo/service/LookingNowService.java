package com.example.demo.service;

import com.example.demo.domain.LookingNow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class LookingNowService {
    private static final Duration PRESENCE_CACHE_TTL = Duration.ofSeconds(30);
    private static final String PRESENCE_CACHE_PREFIX = "looking-now:";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final ReadCache readCache;

    public LookingNowService(
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper,
        UserService userService,
        ReadCache readCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.readCache = readCache;
    }

    public void put(long userId, List<?> subjects) {
        Instant now = Instant.now();
        LookingNow presence = new LookingNow(
            userId,
            LookingNow.validate(subjects),
            now.plus(Duration.ofHours(2)).toString());
        jdbcTemplate.update(
            "INSERT INTO looking_now(user_id,subjects_json,expires_at,created_at) VALUES(?,?,?,?) "
                + "ON CONFLICT(user_id) DO UPDATE SET subjects_json=excluded.subjects_json, "
                + "expires_at=excluded.expires_at,created_at=excluded.created_at",
            userId,
            presence.toSubjectsJson(objectMapper),
            presence.expiresAt(),
            now.toString());
        invalidatePresenceReads();
    }

    public List<VisiblePresence> get(long userId) {
        ReadCache.Key<List<VisiblePresence>> cacheKey = ReadCache.Key.of(PRESENCE_CACHE_PREFIX + userId);
        return readCache.getOrLoad(cacheKey, PRESENCE_CACHE_TTL, () -> loadVisiblePresences(userId));
    }

    public void delete(long userId) {
        jdbcTemplate.update("DELETE FROM looking_now WHERE user_id=?", userId);
        invalidatePresenceReads();
    }

    private List<VisiblePresence> loadVisiblePresences(long userId) {
        jdbcTemplate.update("DELETE FROM looking_now WHERE expires_at<=?", Instant.now().toString());
        return jdbcTemplate.query(
            "SELECT user_id,subjects_json,expires_at FROM looking_now WHERE user_id<>? ORDER BY expires_at",
            resultSet -> {
                List<VisiblePresence> visiblePresences = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    LookingNow presence = LookingNow.deserialize(
                        resultSet.getLong(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        objectMapper);
                    visiblePresences.add(VisiblePresence.from(userService.find(presence.userId()).publicProfile(),
                        presence.subjects(), presence.expiresAt()));
                }
                return List.copyOf(visiblePresences);
            }, userId);
    }

    private void invalidatePresenceReads() {
        readCache.invalidatePrefix(PRESENCE_CACHE_PREFIX);
    }

    /** Flat shape retained for the existing looking-now API contract. */
    public record VisiblePresence(
        long id,
        String username,
        String bio,
        String comments,
        String pictureUrl,
        String avatar,
        String major,
        Integer gradYear,
        List<String> classes,
        List<String> studying,
        List<Integer> studyTimes,
        List<String> preferredStudyLocations,
        String createdAt,
        String updatedAt,
        List<String> subjects,
        String expiresAt) {
        private static VisiblePresence from(com.example.demo.domain.Users.PublicProfile profile,
                                    List<String> subjects, String expiresAt) {
            return new VisiblePresence(profile.id(), profile.username(), profile.bio(), profile.comments(),
                profile.pictureUrl(), profile.avatar(), profile.major(), profile.gradYear(), profile.classes(),
                profile.studying(), profile.studyTimes(), profile.preferredStudyLocations(), profile.createdAt(),
                profile.updatedAt(), subjects, expiresAt);
        }
    }
}
