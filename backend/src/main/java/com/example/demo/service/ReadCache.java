package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Small in-process loading cache. Services supply typed keys and loaders while
 * SQLite remains the authoritative store.
 */
@Service
public final class ReadCache {
    private final ConcurrentHashMap<String, CacheEntry> entries;

    public ReadCache() {
        this.entries = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    public <T> T getOrLoad(Key<T> cacheKey, Duration timeToLive, Supplier<T> loader) {
        if (timeToLive.isNegative() || timeToLive.isZero()) {
            throw new IllegalArgumentException("Cache TTL must be positive");
        }

        CacheEntry cacheEntry = entries.compute(cacheKey.value(), (key, existingEntry) -> {
            Instant now = Instant.now();
            if (existingEntry != null && existingEntry.expiresAt().isAfter(now)) {
                return existingEntry;
            }
            return new CacheEntry(loader.get(), now.plus(timeToLive));
        });
        return cast(cacheEntry.value());
    }

    public <T> void put(Key<T> cacheKey, T value, Duration timeToLive) {
        entries.put(cacheKey.value(), new CacheEntry(value, Instant.now().plus(timeToLive)));
    }

    public void invalidate(Key<?> cacheKey) {
        entries.remove(cacheKey.value());
    }

    public void invalidatePrefix(String cacheKeyPrefix) {
        entries.keySet().removeIf(cacheKey -> cacheKey.startsWith(cacheKeyPrefix));
    }

    private record CacheEntry(Object value, Instant expiresAt) {
    }

    public record Key<T>(String value) {
        public Key {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Cache keys must not be blank");
            }
        }

        public static <T> Key<T> of(String value) {
            return new Key<>(value);
        }
    }
}
