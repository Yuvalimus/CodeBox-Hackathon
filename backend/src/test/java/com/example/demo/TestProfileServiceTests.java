package com.example.demo;

import com.example.demo.api.ApiException;
import com.example.demo.domain.Users;
import com.example.demo.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.HashSet;
import java.util.List;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TestProfileServiceTests {
    @Test
    void createsOneHundredCompleteProfilesWithBalancedCourseLoads() {
        UserService users = mock(UserService.class);
        QueuePresenceService queue = mock(QueuePresenceService.class);
        when(users.registerTestProfiles(anyList())).thenAnswer(call -> java.util.stream.LongStream.rangeClosed(1, ((List<?>) call.getArgument(0)).size()).boxed().toList());
        when(users.publicProfile(anyLong())).thenReturn(mock(Users.PublicProfile.class));
        var service = new TestProfileService(users, queue, true);
        assertEquals(100, service.create(100).size());
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<UserService.Registration>> registrations = ArgumentCaptor.forClass((Class) List.class);
        verify(users).registerTestProfiles(registrations.capture());
        verify(queue).joinPermanently(anyList());
        int total = 0;
        var emails = new HashSet<String>();
        var bios = new HashSet<String>();
        for (var profile : registrations.getValue()) {
            assertFalse(profile.major().isBlank());
            assertTrue(profile.bio().length() > 30 && profile.bio().length() < 500);
            assertTrue(profile.classes().size() >= 4 && profile.classes().size() <= 6);
            assertEquals(profile.classes().size(), new HashSet<>(profile.classes()).size());
            assertTrue(profile.classes().stream().allMatch(course -> course.matches("[A-Z]{2,4} [12][0-9]{3}")));
            assertTrue(profile.classes().containsAll(profile.studying()));
            assertTrue(profile.studying().size() >= 1 && profile.studying().size() <= 3);
            assertTrue(profile.gradYear() >= Year.now().getValue() + 3);
            assertTrue(emails.add(profile.email()));
            bios.add(profile.bio());
            total += profile.classes().size();
        }
        assertEquals(500, total);
        assertTrue(bios.size() > 10);
    }

    @Test
    void disabledOrInvalidRequestsNeverWriteProfiles() {
        UserService users = mock(UserService.class);
        QueuePresenceService queue = mock(QueuePresenceService.class);
        assertThrows(ApiException.class, () -> new TestProfileService(users, queue, false).create(100));
        var enabled = new TestProfileService(users, queue, true);
        assertThrows(ApiException.class, () -> enabled.create(0));
        assertThrows(ApiException.class, () -> enabled.create(101));
        verifyNoInteractions(users, queue);
    }
}
