package com.example.demo.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import com.example.demo.api.ApiErrorResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Small in-process guard; use a shared limiter before running multiple instances.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentMap<String, Window> hits = new ConcurrentHashMap<>();
    private final ObjectMapper json;

    public RateLimitFilter(ObjectMapper o) {
        json = o;
    }

    protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c) throws ServletException, IOException {
        String path = r.getRequestURI();
        if (!(path.equals("/register") || path.equals("/login") || path.contains("/accept") || path.contains("/reject") || path.endsWith("/messages"))) {
            c.doFilter(r, s);
            return;
        }
        int max = (path.equals("/register") || path.equals("/login")) ? 10 : 30;
        long now = Instant.now().getEpochSecond() / 60;
        String key = r.getRemoteAddr() + ":" + path;
        Window w = hits.compute(key, (k, old) -> old == null || old.minute() != now ? new Window(now, 1) : new Window(now, old.count() + 1));
        if (w.count() > max) {
            s.setStatus(429);
            s.setContentType("application/json");
            json.writeValue(s.getOutputStream(), ApiErrorResponse.of("rate_limited", "Too many requests"));
            return;
        }
        c.doFilter(r, s);
    }

    private record Window(long minute, int count) {
    }
}
