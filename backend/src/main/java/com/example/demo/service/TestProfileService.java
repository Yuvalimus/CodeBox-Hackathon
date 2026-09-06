package com.example.demo.service;

import com.example.demo.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.Year;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** Generates local development candidates when explicitly enabled by configuration. */
@Service
public class TestProfileService {
    private static final String[] FIRST_NAMES = {"Alex", "Avery", "Casey", "Drew", "Jordan", "Kai", "Morgan", "Quinn", "Riley", "Taylor"};
    private static final String[] LAST_NAMES = {"Chen", "Garcia", "Lee", "Martinez", "Nguyen", "Patel", "Rivera", "Smith", "Williams", "Young"};
    // Lower-division examples from https://catalog.calpoly.edu/courses/ (2026-28).
    // These are plausible test schedules, not official degree plans.
    private record Track(String major, List<String> courses, String interest) { }
    private static final List<Track> TRACKS = List.of(
        new Track("Computer Science", List.of("CSC 2001", "CSC 2200", "MATH 1264", "COMS 1100", "STAT 1110", "ENGL 1101"), "debugging small projects"),
        new Track("Computer Engineering", List.of("CSC 1001", "MATH 1261", "PHYS 1141", "COMS 1100", "CSC 1000", "ENGL 1101"), "building things with code"),
        new Track("Mathematics", List.of("MATH 1264", "CSC 1001", "STAT 1110", "ENGL 1101", "COMS 1100", "PHYS 1141"), "figuring out why a solution works"),
        new Track("Biological Sciences", List.of("BIO 1113", "CHEM 1120", "MATH 1261", "ENGL 1101", "STAT 1110", "COMS 1100"), "drawing out biology concepts"),
        new Track("Business Administration", List.of("BUS 2201", "ECON 2030", "STAT 1110", "COMS 1100", "ENGL 1101", "BUS 1101"), "connecting class topics to everyday businesses"),
        new Track("Mechanical Engineering", List.of("ME 1125", "MATH 1261", "PHYS 1141", "ENGL 1101", "COMS 1100", "ME 1148"), "sketching out design ideas"),
        new Track("Physics", List.of("PHYS 1143", "MATH 1264", "CSC 1001", "COMS 1100", "ENGL 1101", "STAT 1110"), "working through physics problems"),
        new Track("Statistics", List.of("STAT 1110", "MATH 1261", "CSC 1001", "ENGL 1101", "COMS 1100", "ECON 2030"), "making sense of messy data")
    );
    private static final String[] HABITS = {
        "I learn best by talking through one problem at a time.",
        "Usually a quiet studier, but happy to compare notes between problems.",
        "Trying to stay ahead of homework this week. Whiteboards help a lot.",
        "I bring a list of questions and like checking our reasoning together.",
        "Looking for a regular study buddy who is okay with a few coffee breaks.",
        "Practice problems first, then a quick recap of what we missed.",
        "I like explaining ideas out loud and hearing a different approach.",
        "Still finding my study routine. A little company helps me focus.",
        "Happy to trade notes and work independently at the same table.",
        "I prefer a focused session with phones put away."
    };
    private static final String[] NOTES = {"Reviewing lecture notes before the next quiz.", "Working through this week's problem set.", "Could use a second pair of eyes on a tricky concept.", "Catching up on reading and comparing notes.", "Starting exam review early this time.", "Looking for quiet company while I finish homework."};
    private static final String[] LOCATIONS = {"Kennedy Library", "UU Plaza", "Engineering West", "Julian's Cafe"};
    private static final String[] AVATARS = {"sage", "blue", "peach", "lavender"};
    private static final int[] CLASS_COUNTS = {4, 5, 5, 6};

    private final UserService users;
    private final QueuePresenceService queuePresence;
    private final boolean enabled;
    private final SecureRandom random = new SecureRandom();

    public TestProfileService(UserService users, QueuePresenceService queuePresenceService, @Value("${app.test-data-enabled:false}") boolean enabled) {
        this.users = users;
        this.queuePresence = queuePresenceService;
        this.enabled = enabled;
    }

    @Transactional
    public List<com.example.demo.domain.Users.PublicProfile> create(int count) {
        if (!enabled) {
            throw new ApiException(HttpStatus.NOT_FOUND, "not_found", "Resource not found");
        }
        if (count < 1 || count > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_count", "count must be 1 through 100");
        }

        List<com.example.demo.domain.Users.PublicProfile> profiles = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            long userId = users.register(randomProfile(index));
            queuePresence.joinPermanently(userId);
            profiles.add(users.publicProfile(userId));
        }
        return List.copyOf(profiles);
    }

    private UserService.Registration randomProfile(int index) {
        Track track = TRACKS.get(index % TRACKS.size());
        // Rotating the count by cohort avoids tying a particular major to a load.
        int count = CLASS_COUNTS[(index / TRACKS.size() + index) % CLASS_COUNTS.length];
        List<String> classes = new ArrayList<>(track.courses().subList(0, count));
        Collections.shuffle(classes, random);
        List<String> studying = List.copyOf(classes.subList(0, 1 + random.nextInt(3)));
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "");
        String bio = pick(HABITS) + " Outside lectures, I enjoy " + track.interest() + ".";
        return new UserService.Registration(pick(FIRST_NAMES) + " " + pick(LAST_NAMES), UUID.randomUUID() + "test-password",
            "test-" + uniqueSuffix + "@calpoly.edu", bio, pick(NOTES), null, AVATARS[index % AVATARS.length], track.major(),
            Year.now().getValue() + (index % 2 == 0 ? 4 : 3), List.copyOf(classes), studying,
            new int[]{30, 60, 90, 120}[random.nextInt(4)], List.of(pick(LOCATIONS)));
    }

    private String pick(String[] values) {
        return values[random.nextInt(values.length)];
    }
}
