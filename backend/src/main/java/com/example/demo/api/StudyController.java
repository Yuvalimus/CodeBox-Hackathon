package com.example.demo.api;

import com.example.demo.auth.JwtService;
import com.example.demo.service.ChatService;
import com.example.demo.service.ChatWebSocketTicketService;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class StudyController {
    private final UserService users;
    private final JwtService jwt;
    private final RecommendationService recs;
    private final MatchService matches;
    private final ChatService chats;
    private final ChatWebSocketTicketService chatTickets;
    private final LookingNowService looking;
    private final TestProfileService testProfiles;
    private final ProfilePictureService pictures;
    private final QueuePresenceService queuePresence;
    private final JdbcTemplate db;

    public StudyController(UserService userService, JwtService jwtService, RecommendationService recommendationService, MatchService matchService, ChatService chatService, ChatWebSocketTicketService chatWebSocketTicketService, LookingNowService lookingNowService, TestProfileService testProfileService, ProfilePictureService profilePictureService, QueuePresenceService queuePresenceService, JdbcTemplate jdbcTemplate) {
        this.users = userService;
        this.jwt = jwtService;
        this.recs = recommendationService;
        this.matches = matchService;
        this.chats = chatService;
        this.chatTickets = chatWebSocketTicketService;
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

    private String chatId(String rawId) {
        try { return UUID.fromString(rawId).toString(); }
        catch (IllegalArgumentException exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_chat_id", "chatId must be a UUID"); }
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegistrationRequest request) {
        long id = users.register(request.toRegistration());
        return ResponseEntity.status(201).body(new AuthenticationResponse(jwt.issue(id), users.profile(id)));
    }

    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody LoginRequest request) {
        long id = users.login(request.email(), request.password());
        return new AuthenticationResponse(jwt.issue(id), users.profile(id));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        jwt.revoke(authorizationHeader.substring(7));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ws/chat-ticket")
    public ChatTicketResponse chatTicket(HttpServletRequest request) {
        return new ChatTicketResponse(chatTickets.issue(authenticatedUserId(request)));
    }

    @GetMapping("/me")
    public com.example.demo.domain.Users.Profile profile(HttpServletRequest r) {
        return users.profile(authenticatedUserId(r));
    }

    @PatchMapping("/me")
    public com.example.demo.domain.Users.Profile update(HttpServletRequest r, @RequestBody Map<String, Object> b) {
        users.update(authenticatedUserId(r), b);
        return users.profile(authenticatedUserId(r));
    }

    @PostMapping("/queue/heartbeat")
    public QueuePresenceService.Status heartbeat(HttpServletRequest r) {
        return queuePresence.heartbeat(authenticatedUserId(r));
    }

    @PostMapping("/queue")
    public QueuePresenceService.Status joinQueue(HttpServletRequest r) {
        return queuePresence.join(authenticatedUserId(r));
    }

    @GetMapping("/queue")
    public QueuePresenceService.Status queueStatus(HttpServletRequest r) {
        return queuePresence.status(authenticatedUserId(r));
    }

    @DeleteMapping("/queue")
    public ResponseEntity<Void> leaveQueue(HttpServletRequest r) {
        queuePresence.leave(authenticatedUserId(r));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/picture")
    public com.example.demo.domain.Users.Profile uploadPicture(HttpServletRequest r, @RequestParam("file") MultipartFile file) {
        long userId = authenticatedUserId(r);
        pictures.save(userId, file);
        return users.profile(userId);
    }

    @GetMapping("/recommendations")
    public RecommendationService.Response recommendations(HttpServletRequest r, @RequestParam(defaultValue = "20") int limit,
                                                          @RequestParam(defaultValue = "active") String queue) {
        if (limit < 1 || limit > 50)
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_limit", "limit must be 1 through 50");
        RecommendationService.QueueMode mode;
        try { mode = RecommendationService.QueueMode.valueOf(queue.trim().toUpperCase()); }
        catch (IllegalArgumentException exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_queue", "queue must be active or offline"); }
        return new RecommendationService.Response(recs.recommendations(authenticatedUserId(r), limit, mode));
    }

    @PostMapping("/test/profiles")
    public TestProfilesResponse createTestProfiles(HttpServletRequest request, @RequestParam(defaultValue = "100") int count) {
        authenticatedUserId(request);
        return new TestProfilesResponse(testProfiles.create(count));
    }

    @PostMapping("/recommendations/{userId}/accept")
    public ResponseEntity<?> accept(HttpServletRequest r, @PathVariable String userId) {
        Optional<MatchService.Decision> decision = matches.accept(authenticatedUserId(r), positiveId(userId));
        if (decision.isEmpty()) {
            return ResponseEntity.ok(new EmptyResponse());
        }
        return ResponseEntity.ok(decision.get());
    }

    @PostMapping("/recommendations/{userId}/reject")
    public ResponseEntity<Void> reject(HttpServletRequest r, @PathVariable String userId) {
        matches.reject(authenticatedUserId(r), positiveId(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/matches")
    public MatchesResponse allMatches(HttpServletRequest r) {
        chats.removeExpiredChats();
        long authenticatedUserId = authenticatedUserId(r);
        List<MatchResponse> matches = matchesFor(authenticatedUserId).stream()
            .map(users::publicProfile)
            .map(MatchResponse::new)
            .toList();
        return new MatchesResponse(matches);
    }

    @GetMapping("/matches/unread-count")
    public com.example.demo.domain.Chats.UnreadCount unreadMatches(HttpServletRequest r) {
        return chats.unreadCount(authenticatedUserId(r));
    }

    private List<Long> matchesFor(long me) {
        return db.queryForList("SELECT CASE WHEN user_a_id=? THEN user_b_id ELSE user_a_id END FROM matches WHERE user_a_id=? OR user_b_id=? ORDER BY created_at DESC", Long.class, me, me, me);
    }

    @GetMapping("/chats")
    public ChatListResponse chatList(HttpServletRequest r) {
        return new ChatListResponse(chats.chats(authenticatedUserId(r)));
    }

    @GetMapping("/chats/{chatId}")
    public com.example.demo.domain.Chats.Detail chat(HttpServletRequest r, @PathVariable String chatId, @RequestParam(required = false) String cursor) {
        return chats.chat(authenticatedUserId(r), chatId(chatId), cursor);
    }

    @PostMapping("/chats/{chatId}/messages")
    public ResponseEntity<com.example.demo.domain.Chats.PostedMessage> message(HttpServletRequest r, @PathVariable String chatId, @RequestBody MessageRequest request) {
        return ResponseEntity.status(201).body(chats.message(authenticatedUserId(r), chatId(chatId), request.message()));
    }

    @DeleteMapping("/chats/{chatId}")
    public ResponseEntity<Void> unmatch(HttpServletRequest r, @PathVariable String chatId) {
        chats.unmatch(authenticatedUserId(r), chatId(chatId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/looking-now")
    public ResponseEntity<Void> putLooking(HttpServletRequest r, @RequestBody LookingNowRequest request) {
        looking.put(authenticatedUserId(r), request.subjects());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/looking-now")
    public LookingNowResponse getLooking(HttpServletRequest r) {
        return new LookingNowResponse(looking.get(authenticatedUserId(r)));
    }

    @DeleteMapping("/looking-now")
    public ResponseEntity<Void> deleteLooking(HttpServletRequest r) {
        looking.delete(authenticatedUserId(r));
        return ResponseEntity.noContent().build();
    }

    public record RegistrationRequest(String username, String password, String email, String bio, String comments, String pictureUrl, String avatar,
                                      String major, Integer gradYear, List<String> classes, List<String> studying,
                                      Integer studyDurationMinutes, List<String> preferredStudyLocations) {
        public UserService.Registration toRegistration() {
            return new UserService.Registration(username, password, email, bio, comments, pictureUrl, avatar,
                major, gradYear, classes, studying, studyDurationMinutes, preferredStudyLocations);
        }
    }

    public record LoginRequest(String email, String password) {
    }

    public record HealthResponse(String status) { }
    public record EmptyResponse() { }
    public record AuthenticationResponse(String token, com.example.demo.domain.Users.Profile user) { }
    public record ChatTicketResponse(String ticket) { }
    public record TestProfilesResponse(List<com.example.demo.domain.Users.PublicProfile> profiles) { }
    public record LookingNowResponse(List<LookingNowService.VisiblePresence> users) { }

    public record MessageRequest(String message) {
    }

    public record ChatListResponse(List<com.example.demo.domain.Chats.Summary> chats) { }

    public record MatchResponse(com.example.demo.domain.Users.PublicProfile user) { }
    public record MatchesResponse(List<MatchResponse> matches) { }

    public record LookingNowRequest(List<String> subjects) {
    }
}
