package com.example.demo.api;

import com.example.demo.auth.JwtService;
import com.example.demo.service.ChatService;
import com.example.demo.service.LookingNowService;
import com.example.demo.service.MatchService;
import com.example.demo.service.ProfilePictureService;
import com.example.demo.service.QueuePresenceService;
import com.example.demo.service.RecommendationService;
import com.example.demo.service.TestProfileService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class StudyController {
    private final UserService users;
    private final JwtService jwt;
    private final RecommendationService recs;
    private final MatchService matches;
    private final ChatService chats;
    private final LookingNowService looking;
    private final TestProfileService testProfiles;
    private final ProfilePictureService pictures;
    private final QueuePresenceService queuePresence;
    private final JdbcTemplate db;

    public StudyController(UserService userService, JwtService jwtService, RecommendationService recommendationService, MatchService matchService, ChatService chatService, LookingNowService lookingNowService, TestProfileService testProfileService, ProfilePictureService profilePictureService, QueuePresenceService queuePresenceService, JdbcTemplate jdbcTemplate) {
        this.users = userService;
        this.jwt = jwtService;
        this.recs = recommendationService;
        this.matches = matchService;
        this.chats = chatService;
        this.looking = lookingNowService;
        this.testProfiles = testProfileService;
        this.pictures = profilePictureService;
        this.queuePresence = queuePresenceService;
        this.db = jdbcTemplate;
    }

    private long authenticatedUserId(HttpServletRequest request) {
        Object authenticatedUser = request.getAttribute("userId");
        if (!(authenticatedUser instanceof Long))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthenticated", "Bearer token required");
        return (Long) authenticatedUser;
    }

    private long positiveId(String rawId) {
        try {
            long parsedId = Long.parseLong(rawId);
            if (parsedId <= 0) throw new NumberFormatException();
            return parsedId;
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_id", "ID must be a positive integer");
        }
    }

    @GetMapping("/health")
    Map<String, Object> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        long id = users.register(request.toServiceRequest());
        return ResponseEntity.status(201).body(Map.of("token", jwt.issue(id), "user", users.profile(id)));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        long id = users.login(request.email(), request.password());
        return Map.of("token", jwt.issue(id), "user", users.profile(id));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        jwt.revoke(authorizationHeader.substring(7));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    Map<String, Object> profile(HttpServletRequest r) {
        return users.profile(authenticatedUserId(r));
    }

    @PatchMapping("/me")
    Map<String, Object> update(HttpServletRequest r, @RequestBody Map<String, Object> b) {
        users.update(authenticatedUserId(r), b);
        return users.profile(authenticatedUserId(r));
    }

    @PostMapping("/queue/heartbeat")
    Map<String, Object> heartbeat(HttpServletRequest r) {
        return queuePresence.heartbeat(authenticatedUserId(r));
    }

    @PostMapping("/queue")
    Map<String, Object> joinQueue(HttpServletRequest r) {
        return queuePresence.join(authenticatedUserId(r));
    }

    @GetMapping("/queue")
    Map<String, Object> queueStatus(HttpServletRequest r) {
        return queuePresence.status(authenticatedUserId(r));
    }

    @DeleteMapping("/queue")
    ResponseEntity<Void> leaveQueue(HttpServletRequest r) {
        queuePresence.leave(authenticatedUserId(r));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/picture")
    Map<String, Object> uploadPicture(HttpServletRequest r, @RequestParam("file") MultipartFile file) {
        long userId = authenticatedUserId(r);
        pictures.save(userId, file);
        return users.profile(userId);
    }

    @GetMapping("/recommendations")
    Map<String, Object> recommendations(HttpServletRequest r, @RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > 50)
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_limit", "limit must be 1 through 50");
        return Map.of("recommendations", recs.recommendations(authenticatedUserId(r), limit));
    }

    @PostMapping("/test/profiles")
    Map<String, Object> createTestProfiles(HttpServletRequest request, @RequestParam(defaultValue = "10") int count) {
        authenticatedUserId(request);
        return Map.of("profiles", testProfiles.create(count));
    }

    @PostMapping("/recommendations/{userId}/accept")
    Map<String, Object> accept(HttpServletRequest r, @PathVariable String userId) {
        return matches.accept(authenticatedUserId(r), positiveId(userId));
    }

    @PostMapping("/recommendations/{userId}/reject")
    ResponseEntity<Void> reject(HttpServletRequest r, @PathVariable String userId) {
        matches.reject(authenticatedUserId(r), positiveId(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/matches")
    Map<String, Object> allMatches(HttpServletRequest r) {
        chats.removeExpiredChats();
        long authenticatedUserId = authenticatedUserId(r);
        List<Map<String, Object>> o = new ArrayList<>();
        for (Long other : matchesFor(authenticatedUserId)) {
            Map<String, Object> p = users.profile(other);
            p.remove("email");
            o.add(Map.of("user", p));
        }
        return Map.of("matches", o);
    }

    private List<Long> matchesFor(long me) {
        return db.queryForList("SELECT CASE WHEN user_a_id=? THEN user_b_id ELSE user_a_id END FROM matches WHERE user_a_id=? OR user_b_id=? ORDER BY created_at DESC", Long.class, me, me, me);
    }

    @GetMapping("/chats")
    Map<String, Object> chatList(HttpServletRequest r) {
        return Map.of("chats", chats.chats(authenticatedUserId(r)).stream().map(com.example.demo.domain.Chats.Summary::serialize).toList());
    }

    @GetMapping("/chats/{chatId}")
    Map<String, Object> chat(HttpServletRequest r, @PathVariable String chatId, @RequestParam(required = false) String cursor) {
        return chats.chat(authenticatedUserId(r), positiveId(chatId), cursor);
    }

    @PostMapping("/chats/{chatId}/messages")
    public ResponseEntity<?> message(HttpServletRequest r, @PathVariable String chatId, @RequestBody MessageRequest request) {
        return ResponseEntity.status(201).body(chats.message(authenticatedUserId(r), positiveId(chatId), request.message()));
    }

    @PutMapping("/looking-now")
    public ResponseEntity<Void> putLooking(HttpServletRequest r, @RequestBody LookingNowRequest request) {
        looking.put(authenticatedUserId(r), request.subjects());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/looking-now")
    public Map<String, Object> getLooking(HttpServletRequest r) {
        return Map.of("users", looking.get(authenticatedUserId(r)));
    }

    @DeleteMapping("/looking-now")
    public ResponseEntity<Void> deleteLooking(HttpServletRequest r) {
        looking.delete(authenticatedUserId(r));
        return ResponseEntity.noContent().build();
    }

    public record RegistrationRequest(String username, String password, String email, String bio, String comments, String pictureUrl,
                                      String major, Integer gradYear, List<String> classes, List<String> studying,
                                      List<Integer> studyTimes, List<String> preferredStudyLocations) {
        public Map<String, Object> toServiceRequest() {
            Map<String, Object> values = new HashMap<>();
            values.put("username", username);
            values.put("password", password);
            values.put("email", email);
            values.put("bio", bio);
            values.put("comments", comments);
            values.put("pictureUrl", pictureUrl);
            values.put("major", major);
            values.put("gradYear", gradYear);
            values.put("classes", classes);
            values.put("studying", studying);
            values.put("studyTimes", studyTimes);
            values.put("preferredStudyLocations", preferredStudyLocations);
            return values;
        }
    }

    public record LoginRequest(String email, String password) {
    }

    public record MessageRequest(String message) {
    }

    public record LookingNowRequest(List<String> subjects) {
    }
}
