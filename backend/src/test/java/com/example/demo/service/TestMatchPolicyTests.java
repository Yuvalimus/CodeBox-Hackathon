package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TestMatchPolicyTests {
    @Test void exactlyThirtyOfOneHundredDrawsAcceptGeneratedProfiles() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        when(db.queryForObject(anyString(), eq(Boolean.class), eq(7L))).thenReturn(true);
        int accepted = 0;
        for (int draw = 0; draw < 100; draw++) {
            int value = draw;
            if (new TestMatchPolicy(db, true, () -> value).accepts(7)) accepted++;
        }
        assertEquals(30, accepted);
    }
    @Test void ordinaryUsersAndDisabledEnvironmentsNeverAutoAccept() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        when(db.queryForObject(anyString(), eq(Boolean.class), eq(8L))).thenReturn(false);
        assertFalse(new TestMatchPolicy(db, true, () -> { throw new AssertionError(); }).accepts(8));
        reset(db);
        assertFalse(new TestMatchPolicy(db, false, () -> 0).accepts(7));
        verifyNoInteractions(db);
    }
}
