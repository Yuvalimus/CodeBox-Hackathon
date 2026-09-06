package com.example.demo;

import com.example.demo.api.ApiException;
import com.example.demo.domain.Users;
import com.example.demo.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.HashSet;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TestProfileServiceTests {
    @Test
    void createsOneHundredCompleteProfilesWithBalancedCourseLoads() {
        UserService users = mock(UserService.class);
        QueuePresenceService queue = mock(QueuePresenceService.class);
        var nextId = new java.util.concurrent.atomic.AtomicLong();
        when(users.register(any())).thenAnswer(call -> nextId.incrementAndGet());
        when(users.publicProfile(anyLong())).thenReturn(mock(Users.PublicProfile.class));
        var service = new TestProfileService(users, queue, true);
        assertEquals(100, service.create(100).size());
        var registrations = ArgumentCaptor.forClass(UserService.Registration.class);
        verify(users, times(100)).register(registrations.capture());
        verify(queue, times(100)).joinPermanently(anyLong());
        int total = 0;
        var emails = new HashSet<String>();
        var bios = new HashSet<String>();
        for (var profile : registrations.getAllValues()) {
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
