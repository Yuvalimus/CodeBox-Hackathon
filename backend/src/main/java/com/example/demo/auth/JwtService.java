package com.example.demo.auth;

import com.example.demo.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {
    private final String secret, issuer, audience;
    private final ObjectMapper json;

    public JwtService(@Value("${app.jwt-secret}") String s, @Value("${app.jwt-issuer}") String i, @Value("${app.jwt-audience}") String a, ObjectMapper j) {
        secret = s;
        issuer = i;
        audience = a;
        json = j;
    }

    public String issue(long id) {
        try {
            long exp = Instant.now().plus(Duration.ofDays(7)).getEpochSecond();
            String h = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String p = b64(json.writeValueAsString(Map.of("sub", String.valueOf(id), "iss", issuer, "aud", audience, "exp", exp)));
            return h + "." + p + "." + sign(h + "." + p);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public long verify(String token) {
        try {
            String[] x = token.split("\\.");
            if (x.length != 3 || !constant(sign(x[0] + "." + x[1]), x[2])) throw bad();
            Map<String, Object> p = json.readValue(Base64.getUrlDecoder().decode(x[1]), Map.class);
            if (!issuer.equals(p.get("iss")) || !audience.equals(p.get("aud")) || ((Number) p.get("exp")).longValue() < Instant.now().getEpochSecond())
                throw bad();
            return Long.parseLong((String) p.get("sub"));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw bad();
        }
    }

    private String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String s) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(m.doFinal(s.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constant(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }

    private ApiException bad() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_token", "Invalid or expired access token");
    }
}
