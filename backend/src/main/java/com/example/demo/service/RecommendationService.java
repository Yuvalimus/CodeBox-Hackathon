package com.example.demo.service;

import com.example.demo.domain.Users;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RecommendationService {
    private static final Duration RECOMMENDATION_CACHE_TTL = Duration.ofMinutes(15);
    private static final String RECOMMENDATION_CACHE_PREFIX = "recommendations:";

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final ReadCache readCache;

    public RecommendationService(JdbcTemplate jdbcTemplate, UserService userService, ReadCache readCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.readCache = readCache;
    }

    public static double score(Map<String, Object> firstProfile, Map<String, Object> secondProfile) {
        return score(
            valueSet(firstProfile, "studying"),
            valueSet(secondProfile, "studying"),
            valueSet(firstProfile, "studyTimes"),
            valueSet(secondProfile, "studyTimes"),
            firstProfile.get("gradYear"),
            secondProfile.get("gradYear"));
    }

    public static double score(Users firstUser, Users secondUser) {
        return score(
            new HashSet<>(firstUser.studying()),
            new HashSet<>(secondUser.studying()),
            new HashSet<>(firstUser.studyTimes()),
            new HashSet<>(secondUser.studyTimes()),
            firstUser.gradYear(),
            secondUser.gradYear());
    }

    private static double score(
        Set<Object> firstStudying,
        Set<Object> secondStudying,
        Set<Object> firstStudyTimes,
        Set<Object> secondStudyTimes,
        Object firstGraduationYear,
        Object secondGraduationYear) {
        return 0.70 * jaccardOverlap(firstStudying, secondStudying)
            + 0.15 * timeOverlap(firstStudyTimes, secondStudyTimes)
            + 0.15 * yearSimilarity(firstGraduationYear, secondGraduationYear);
    }

    private static Set<Object> valueSet(Map<String, Object> profile, String fieldName) {
        Object rawValue = profile.get(fieldName);
        if (!(rawValue instanceof List<?> values)) {
            return Set.of();
        }
        return new HashSet<>(values);
    }

    private static double jaccardOverlap(Set<Object> firstValues, Set<Object> secondValues) {
        if (firstValues.isEmpty() && secondValues.isEmpty()) {
            return 0;
        }
        Set<Object> union = new HashSet<>(firstValues);
        union.addAll(secondValues);
        Set<Object> intersection = new HashSet<>(firstValues);
        intersection.retainAll(secondValues);
        return (double) intersection.size() / union.size();
    }

    private static double timeOverlap(Set<Object> firstValues, Set<Object> secondValues) {
        if (firstValues.isEmpty() || secondValues.isEmpty()) {
            return 0;
        }
        Set<Object> intersection = new HashSet<>(firstValues);
        intersection.retainAll(secondValues);
        return (double) intersection.size() / Math.min(firstValues.size(), secondValues.size());
    }

    private static double yearSimilarity(Object firstYear, Object secondYear) {
        if (!(firstYear instanceof Number firstNumber) || !(secondYear instanceof Number secondNumber)) {
            return 0;
        }
        int yearDifference = Math.abs(firstNumber.intValue() - secondNumber.intValue());
        return yearDifference == 0 ? 1 : Math.max(0, 1 - yearDifference / 4.0);
    }

    public List<Map<String, Object>> recommendations(long userId, int limit) {
        ReadCache.Key<List<Map<String, Object>>> cacheKey = ReadCache.Key.of(RECOMMENDATION_CACHE_PREFIX + userId + ":" + limit);
        return readCache.getOrLoad(cacheKey, RECOMMENDATION_CACHE_TTL, () -> loadRecommendations(userId, limit));
    }

    private List<Map<String, Object>> loadRecommendations(long userId, int limit) {
        Users currentUser = userService.find(userId);
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Long candidateId : eligibleCandidateIds(userId)) {
            Users candidate = userService.find(candidateId);
            Map<String, Object> serializedCandidate = candidate.serialize(false);
            serializedCandidate.remove("createdAt");
            serializedCandidate.remove("updatedAt");
            serializedCandidate.put("compatibility", score(currentUser, candidate));
            recommendations.add(serializedCandidate);
        }
        recommendations.sort(Comparator
            .<Map<String, Object>, Double>comparing(recommendation -> -(Double) recommendation.get("compatibility"))
            .thenComparing(recommendation -> (Long) recommendation.get("id")));
        return List.copyOf(recommendations.subList(0, Math.min(limit, recommendations.size())));
    }

    private List<Long> eligibleCandidateIds(long userId) {
        return jdbcTemplate.queryForList(
            "SELECT id FROM users WHERE id<>? "
                + "AND id NOT IN (SELECT target_user_id FROM match_decisions WHERE actor_user_id=?) "
                + "AND id NOT IN (SELECT CASE WHEN user_a_id=? THEN user_b_id ELSE user_a_id END "
                + "FROM matches WHERE user_a_id=? OR user_b_id=?) ORDER BY id",
            Long.class, userId, userId, userId, userId, userId);
    }
}
