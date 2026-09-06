package com.example.demo.service;

import com.example.demo.domain.Users;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecommendationService {
    private static final Duration RECOMMENDATION_CACHE_TTL = Duration.ofSeconds(5);
    private static final String RECOMMENDATION_CACHE_PREFIX = "recommendations:";
    private static final Pattern COURSE_CODE = Pattern.compile("^([A-Z]{2,4})\\s*(\\d{4})$");

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final ChatService chats;
    private final QueuePresenceService queuePresence;
    private final ReadCache readCache;

    public RecommendationService(JdbcTemplate jdbcTemplate, UserService userService, ChatService chatService, QueuePresenceService queuePresenceService, ReadCache readCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.chats = chatService;
        this.queuePresence = queuePresenceService;
        this.readCache = readCache;
    }

    public record CompatibilityProfile(Set<String> studying, Integer studyDurationMinutes, Integer gradYear) {
        public CompatibilityProfile {
            studying = Set.copyOf(studying);
        }
    }

    public enum QueueMode { ACTIVE, OFFLINE }

    public record Candidate(
        long id,
        String username,
        String bio,
        String comments,
        String pictureUrl,
        String avatar,
        String major,
        Integer gradYear,
        List<String> classes,
        List<String> sharedClasses,
        List<String> studying,
        Integer studyDurationMinutes,
        List<String> preferredStudyLocations,
        double compatibility) {
        private static Candidate from(Users user, double compatibility, List<String> sharedClasses) {
            Users.Profile profile = user.profile(false);
            return new Candidate(profile.id(), profile.username(), profile.bio(), profile.comments(), profile.pictureUrl(),
                profile.avatar(), profile.major(), profile.gradYear(), profile.classes(), sharedClasses, profile.studying(),
                profile.studyDurationMinutes(), profile.preferredStudyLocations(), compatibility);
        }
    }

    public record Response(List<Candidate> recommendations) {
        public Response {
            recommendations = List.copyOf(recommendations);
        }
    }

    public static double score(CompatibilityProfile firstProfile, CompatibilityProfile secondProfile) {
        return score(firstProfile.studying(), secondProfile.studying(), firstProfile.studyDurationMinutes(),
            secondProfile.studyDurationMinutes(), firstProfile.gradYear(), secondProfile.gradYear());
    }

    public static double score(Users firstUser, Users secondUser) {
        return score(new CompatibilityProfile(
            new HashSet<>(firstUser.studying()),
            firstUser.studyDurationMinutes(),
            firstUser.gradYear()),
            new CompatibilityProfile(
                new HashSet<>(secondUser.studying()),
                secondUser.studyDurationMinutes(),
                secondUser.gradYear()));
    }

    private static double score(
        Set<String> firstStudying,
        Set<String> secondStudying,
        Integer firstStudyDurationMinutes,
        Integer secondStudyDurationMinutes,
        Integer firstGraduationYear,
        Integer secondGraduationYear) {
        return 0.70 * jaccardOverlap(firstStudying, secondStudying)
            + 0.15 * durationSimilarity(firstStudyDurationMinutes, secondStudyDurationMinutes)
            + 0.15 * yearSimilarity(firstGraduationYear, secondGraduationYear);
    }

    private static double jaccardOverlap(Set<String> firstValues, Set<String> secondValues) {
        if (firstValues.isEmpty() && secondValues.isEmpty()) {
            return 0;
        }
        Set<Object> union = new HashSet<>(firstValues);
        union.addAll(secondValues);
        Set<Object> intersection = new HashSet<>(firstValues);
        intersection.retainAll(secondValues);
        return (double) intersection.size() / union.size();
    }

    private static double durationSimilarity(Integer firstDuration, Integer secondDuration) {
        if (firstDuration == null || secondDuration == null) {
            return 0;
        }
        return (double) Math.min(firstDuration, secondDuration) / Math.max(firstDuration, secondDuration);
    }

    private static double yearSimilarity(Integer firstYear, Integer secondYear) {
        if (firstYear == null || secondYear == null) {
            return 0;
        }
        int yearDifference = Math.abs(firstYear - secondYear);
        return yearDifference == 0 ? 1 : Math.max(0, 1 - yearDifference / 4.0);
    }

    public List<Candidate> recommendations(long userId, int limit, QueueMode mode) {
        chats.removeExpiredChats();
        queuePresence.removeExpiredPresences();
        ReadCache.Key<List<Candidate>> cacheKey = ReadCache.Key.of(RECOMMENDATION_CACHE_PREFIX + mode + ":" + userId + ":" + limit);
        return readCache.getOrLoad(cacheKey, RECOMMENDATION_CACHE_TTL, () -> loadRecommendations(userId, limit, mode));
    }

    private List<Candidate> loadRecommendations(long userId, int limit, QueueMode mode) {
        Users currentUser = userService.find(userId);
        Set<Long> usersWhoAcceptedMe = usersWhoAccepted(userId);
        Set<String> currentClasses = matchingClasses(currentUser, mode);
        List<Candidate> recommendations = new ArrayList<>();
        for (Long candidateId : eligibleCandidateIds(userId, mode)) {
            Users candidate = userService.find(candidateId);
            if (mode == QueueMode.ACTIVE && (currentClasses.isEmpty() || java.util.Collections.disjoint(currentClasses, matchingClasses(candidate, mode)))) {
                continue;
            }
            recommendations.add(Candidate.from(candidate, score(currentUser, candidate, mode), sharedClasses(currentUser, candidate, mode)));
        }
        recommendations.sort(Comparator
            .comparing((Candidate recommendation) -> !usersWhoAcceptedMe.contains(recommendation.id()))
            .thenComparing(Comparator.comparingDouble(Candidate::compatibility).reversed())
            .thenComparingLong(Candidate::id));
        return List.copyOf(recommendations.subList(0, Math.min(limit, recommendations.size())));
    }

    private double score(Users firstUser, Users secondUser, QueueMode mode) {
        if (mode == QueueMode.ACTIVE) {
            return score(firstUser, secondUser);
        }
        return score(new CompatibilityProfile(
            matchingClasses(firstUser, mode), firstUser.studyDurationMinutes(), firstUser.gradYear()),
            new CompatibilityProfile(
                matchingClasses(secondUser, mode), secondUser.studyDurationMinutes(), secondUser.gradYear()));
    }

    private Set<String> matchingClasses(Users user, QueueMode mode) {
        List<String> values = mode == QueueMode.ACTIVE ? user.studying() : user.classes();
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            String course = value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
            Matcher matcher = COURSE_CODE.matcher(course);
            normalized.add(matcher.matches() ? matcher.group(1) + " " + matcher.group(2) : course);
        }
        return normalized;
    }

    private List<String> sharedClasses(Users currentUser, Users candidate, QueueMode mode) {
        Set<String> currentClasses = matchingClasses(currentUser, mode);
        List<String> candidateClasses = mode == QueueMode.ACTIVE ? candidate.studying() : candidate.classes();
        return candidateClasses.stream()
            .filter(course -> currentClasses.contains(normalizedCourse(course)))
            .toList();
    }

    private String normalizedCourse(String value) {
        String course = value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        Matcher matcher = COURSE_CODE.matcher(course);
        return matcher.matches() ? matcher.group(1) + " " + matcher.group(2) : course;
    }

    private Set<Long> usersWhoAccepted(long userId) {
        return new HashSet<>(jdbcTemplate.queryForList(
            "SELECT actor_user_id FROM match_decisions WHERE target_user_id=? "
                + "AND decision IN ('accepted','deferred') AND created_at>?",
            Long.class, userId, java.time.Instant.now().minus(MatchService.DECISION_TTL).toString()));
    }

    private List<Long> eligibleCandidateIds(long userId, QueueMode mode) {
        String decisionCutoff = java.time.Instant.now().minus(MatchService.DECISION_TTL).toString();
        String queueEligibility = mode == QueueMode.ACTIVE
            ? "(EXISTS (SELECT 1 FROM user_queue_presence queue_presence WHERE queue_presence.user_id=users.id AND queue_presence.last_heartbeat_at>?) OR EXISTS (SELECT 1 FROM permanent_test_queue_users permanent_test_user WHERE permanent_test_user.user_id=users.id))"
            : "1=1";
        Object[] parameters = mode == QueueMode.ACTIVE
            ? new Object[]{userId, java.time.Instant.now().minus(QueuePresenceService.OFFLINE_AFTER).toString(),
                userId, decisionCutoff, userId, userId, userId}
            : new Object[]{userId, userId, decisionCutoff, userId, userId, userId};
        return jdbcTemplate.queryForList(
            "SELECT users.id FROM users WHERE users.id<>? "
                + "AND " + queueEligibility + " "
                + "AND users.id NOT IN (SELECT target_user_id FROM match_decisions WHERE actor_user_id=? "
                + "AND created_at>?) "
                + "AND users.id NOT IN (SELECT CASE WHEN user_a_id=? THEN user_b_id ELSE user_a_id END "
                + "FROM matches WHERE user_a_id=? OR user_b_id=?) "
                + "ORDER BY users.id",
            Long.class, parameters);
    }
}
