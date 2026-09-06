package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/** Issues short-lived, single-use tickets for browser WebSocket handshakes. */
@Service
public class ChatWebSocketTicketService {
    private static final Duration TICKET_TTL = Duration.ofMinutes(1);
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String issue(long userId) {
        removeExpiredTickets();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(value, new Ticket(userId, Instant.now().plus(TICKET_TTL)));
        return value;
    }

    /** Atomically consumes a valid ticket so it cannot be replayed. */
    public OptionalLong consume(String value) {
        if (value == null || value.isBlank()) {
            return OptionalLong.empty();
        }
        Ticket ticket = tickets.remove(value);
        if (ticket == null || !ticket.expiresAt().isAfter(Instant.now())) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(ticket.userId());
    }

    private void removeExpiredTickets() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private record Ticket(long userId, Instant expiresAt) { }
}
