package com.example.demo.service;

import com.example.demo.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final boolean enabled;
    private final SecureRandom random = new SecureRandom();

    public TestProfileService(UserService users, @Value("${app.test-data-enabled:false}") boolean enabled) {
        this.users = users;
        this.enabled = enabled;
    }

    public List<Map<String, Object>> create(int count) {
        if (!enabled) {
            throw new ApiException(HttpStatus.NOT_FOUND, "not_found", "Resource not found");
        }
        if (count < 1 || count > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_count", "count must be 1 through 50");
        }

        List<Map<String, Object>> profiles = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            long userId = users.register(randomProfile());
            Map<String, Object> profile = users.profile(userId);
            profile.remove("email");
            profiles.add(profile);
        }
        return List.copyOf(profiles);
    }

    private Map<String, Object> randomProfile() {
        String firstName = pick(FIRST_NAMES);
        String lastName = pick(LAST_NAMES);
        String primaryClass = pick(CLASSES);
        String secondaryClass = pick(CLASSES);
        List<String> classes = primaryClass.equals(secondaryClass) ? List.of(primaryClass) : List.of(primaryClass, secondaryClass);
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "");
        return Map.of(
            "username", firstName + " " + lastName,
            "email", "test-" + uniqueSuffix + "@calpoly.edu",
            "password", UUID.randomUUID() + "test-password",
            "bio", "Generated test profile for local development.",
            "major", pick(MAJORS),
            "gradYear", 2026 + random.nextInt(5),
            "classes", classes,
            "studying", List.of(primaryClass),
            "studyTimes", List.of(random.nextInt(168), random.nextInt(168)),
            "preferredStudyLocations", List.of(pick(LOCATIONS)));
    }

    private String pick(String[] values) {
        return values[random.nextInt(values.length)];
    }
}
