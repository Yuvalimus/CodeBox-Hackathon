package com.example.demo.api;

import com.example.demo.auth.JwtService;
import com.example.demo.service.ChatEventSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {
    private final ChatEventSocketHandler handler;
    private final JwtService jwt;
    private final String[] origins;

    public ChatWebSocketConfig(ChatEventSocketHandler handler, JwtService jwt,
                               @Value("${app.cors-origins}") String configuredOrigins) {
        this.handler = handler;
        this.jwt = jwt;
        this.origins = Arrays.stream(configuredOrigins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat")
            .addInterceptors(new TokenHandshakeInterceptor(jwt))
            .setAllowedOrigins(origins);
    }

    private static final class TokenHandshakeInterceptor implements HandshakeInterceptor {
        private final JwtService jwt;

        private TokenHandshakeInterceptor(JwtService jwt) {
            this.jwt = jwt;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String token = queryParameter(request.getURI(), "token");
            if (token == null) return false;
            try {
                attributes.put("userId", jwt.verify(token));
                return true;
            } catch (RuntimeException exception) {
                return false;
            }
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }

        private static String queryParameter(URI uri, String name) {
            String query = uri.getRawQuery();
            if (query == null) return null;
            return Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .filter(part -> part.length == 2 && name.equals(part[0]))
                .map(part -> java.net.URLDecoder.decode(part[1], java.nio.charset.StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);
        }
    }
}
