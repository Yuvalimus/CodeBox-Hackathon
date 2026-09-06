package com.example.demo.service;

import com.example.demo.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/** Generates local development candidates when explicitly enabled by configuration. */
@Service
public class TestProfileService {
    private static final String[] FIRST_NAMES = {"Alex", "Avery", "Casey", "Drew", "Jordan", "Kai", "Morgan", "Quinn", "Riley", "Taylor"};
    private static final String[] LAST_NAMES = {"Chen", "Garcia", "Lee", "Martinez", "Nguyen", "Patel", "Rivera", "Smith", "Williams", "Young"};
    private static final String[] MAJORS = {"Computer Science", "Mathematics", "Biology", "Business Administration", "Mechanical Engineering"};
    private static final String[] CLASSES = {"CSC 1000", "CSC 1001", "STAT 3120", "MATH 1000", "PHYS 1001", "BIO 1002"};
    private static final String[] LOCATIONS = {"Kennedy Library", "UU Plaza", "Engineering West", "Julian's Cafe"};

    private final UserService users;
    private final QueuePresenceService queuePresence;
    private final boolean enabled;
    private final SecureRandom random = new SecureRandom();

    public TestProfileService(UserService users, QueuePresenceService queuePresenceService, @Value("${app.test-data-enabled:false}") boolean enabled) {
        this.users = users;
        this.queuePresence = queuePresenceService;
        this.enabled = enabled;
    }

    public List<com.example.demo.domain.Users.PublicProfile> create(int count) {
        if (!enabled) {
            throw new ApiException(HttpStatus.NOT_FOUND, "not_found", "Resource not found");
        }
        if (count < 1 || count > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_count", "count must be 1 through 50");
        }

        List<com.example.demo.domain.Users.PublicProfile> profiles = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            long userId = users.register(randomProfile());
            queuePresence.joinPermanently(userId);
            profiles.add(users.publicProfile(userId));
        }
        return List.copyOf(profiles);
    }

    private UserService.Registration randomProfile() {
        String firstName = pick(FIRST_NAMES);
        String lastName = pick(LAST_NAMES);
        String primaryClass = pick(CLASSES);
        String secondaryClass = pick(CLASSES);
        List<String> classes = primaryClass.equals(secondaryClass) ? List.of(primaryClass) : List.of(primaryClass, secondaryClass);
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "");
        return new UserService.Registration(firstName + " " + lastName, UUID.randomUUID() + "test-password",
            "test-" + uniqueSuffix + "@calpoly.edu", "Generated test profile for local development.",
            "Looking for a focused one-hour study session.", null, "sage", pick(MAJORS),
            2026 + random.nextInt(5), classes, List.of(primaryClass),
            List.of(random.nextInt(168), random.nextInt(168)), List.of(pick(LOCATIONS)));
    }

    private String pick(String[] values) {
        return values[random.nextInt(values.length)];
    }
}
