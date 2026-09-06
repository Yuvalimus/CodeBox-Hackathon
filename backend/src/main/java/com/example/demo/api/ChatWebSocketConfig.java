package com.example.demo.api;

import com.example.demo.service.ChatEventSocketHandler;
import com.example.demo.service.ChatWebSocketTicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketConfig.class);
    private final ChatEventSocketHandler handler;
    private final ChatWebSocketTicketService tickets;
    private final String[] origins;

    public ChatWebSocketConfig(ChatEventSocketHandler handler, ChatWebSocketTicketService ticketService,
                               @Value("${app.cors-origins}") String configuredOrigins) {
        this.handler = handler;
        this.tickets = ticketService;
        this.origins = Arrays.stream(configuredOrigins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat")
            .addInterceptors(new TicketHandshakeInterceptor(tickets))
            .setAllowedOrigins(origins);
    }

    private static final class TicketHandshakeInterceptor implements HandshakeInterceptor {
        private final ChatWebSocketTicketService tickets;

        private TicketHandshakeInterceptor(ChatWebSocketTicketService tickets) {
            this.tickets = tickets;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String ticket = queryParameter(request.getURI(), "ticket");

            if (ticket == null) {
                log.warn("Rejected chat WebSocket handshake without a ticket");
                return false;
            }

            var userId = tickets.consume(ticket);
            if (userId.isPresent()) {
                attributes.put("userId", userId.getAsLong());
                log.info("Authenticated chat WebSocket handshake for user {}", userId.getAsLong());
                return true;
            }
            log.warn("Rejected chat WebSocket handshake with an invalid or expired ticket");
            return false;
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
