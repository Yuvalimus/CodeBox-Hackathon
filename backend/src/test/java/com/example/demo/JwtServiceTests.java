package com.example.demo;

import com.example.demo.api.ApiException;
import com.example.demo.auth.JwtService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTests {
    @Test
    void revokedTokenCannotBeUsedAgain() {
        JwtService jwt = new JwtService("a-development-secret-that-is-long-enough", "issuer", "audience", new ObjectMapper());
        String token = jwt.issue(42);

        assertEquals(42, jwt.verify(token));
        jwt.revoke(token);
        assertThrows(ApiException.class, () -> jwt.verify(token));
    }
}
