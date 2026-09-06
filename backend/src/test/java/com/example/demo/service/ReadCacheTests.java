package com.example.demo.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadCacheTests {
    @Test
    void permitsProfileLoadsWithinRecommendationLoads() {
        ReadCache cache = new ReadCache();
        String result = cache.getOrLoad(ReadCache.Key.of("recommendations:1"), Duration.ofSeconds(5),
            () -> cache.getOrLoad(ReadCache.Key.of("profile:2"), Duration.ofMinutes(1), () -> "candidate"));

        assertEquals("candidate", result);
    }
}
