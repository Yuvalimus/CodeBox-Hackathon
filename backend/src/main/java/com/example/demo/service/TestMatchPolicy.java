package com.example.demo.service;

import java.security.SecureRandom;
import java.util.function.IntSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestMatchPolicy {
    private final JdbcTemplate database;
    private final boolean enabled;
    private final IntSupplier draw;

    @Autowired
    public TestMatchPolicy(JdbcTemplate database, @Value("${app.test-data-enabled:false}") boolean enabled) {
        this(database, enabled, () -> new SecureRandom().nextInt(100));
    }

    TestMatchPolicy(JdbcTemplate database, boolean enabled, IntSupplier draw) {
        this.database = database;
        this.enabled = enabled;
        this.draw = draw;
    }

    public boolean accepts(long targetId) {
        if (!enabled) return false;
        // Use the generator's database marker, never names or email patterns.
        Boolean generated = database.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM permanent_test_queue_users WHERE user_id=?)", Boolean.class, targetId);
        return Boolean.TRUE.equals(generated) && draw.getAsInt() < 30;
    }
}
