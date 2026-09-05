package com.example.demo.auth;

import com.example.demo.api.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final ObjectMapper json;

    public AuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwt = jwtService;
        this.json = objectMapper;
    }

    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return requestPath.equals("/health") || requestPath.equals("/register") || requestPath.equals("/login")
            || requestPath.startsWith("/uploads/profile-pictures/");
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthenticated", "Bearer token required");
            }
            request.setAttribute("userId", jwt.verify(authorizationHeader.substring(7)));
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            response.setStatus(exception.status.value());
            response.setContentType("application/json");
            json.writeValue(response.getOutputStream(), Map.of("error", Map.of("code", exception.code, "message", exception.getMessage())));
        }
    }
}
