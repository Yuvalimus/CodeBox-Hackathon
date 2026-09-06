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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class JwtService {
    private final String secret, issuer, audience;
    private final ObjectMapper json;
    private final ConcurrentMap<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public JwtService(@Value("${app.jwt-secret}") String secret, @Value("${app.jwt-issuer}") String issuer, @Value("${app.jwt-audience}") String audience, ObjectMapper json) {
        this.secret = secret;
        this.issuer = issuer;
        this.audience = audience;
        this.json = json;
    }

    public String issue(long id) {
        try {
            long exp = Instant.now().plus(Duration.ofDays(7)).getEpochSecond();
            String h = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String p = b64(json.writeValueAsString(new TokenPayload(String.valueOf(id), issuer, audience, exp)));
            return h + "." + p + "." + sign(h + "." + p);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public long verify(String token) {
        try {
            String[] x = token.split("\\.");
            if (x.length != 3 || !constant(sign(x[0] + "." + x[1]), x[2])) throw bad();
            TokenPayload payload = readPayload(x[1]);
            if (revokedTokens.containsKey(tokenFingerprint(token)) || !issuer.equals(payload.iss()) || !audience.equals(payload.aud()) || payload.exp() < Instant.now().getEpochSecond())
                throw bad();
            return Long.parseLong(payload.sub());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw bad();
        }
    }

    /** Revokes this specific access token until it would naturally expire. */
    public void revoke(String token) {
        verify(token);
        try {
            String[] parts = token.split("\\.");
            TokenPayload payload = readPayload(parts[1]);
            revokedTokens.put(tokenFingerprint(token), payload.exp());
            long now = Instant.now().getEpochSecond();
            revokedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
        } catch (Exception exception) {
            throw bad();
        }
    }

    private String tokenFingerprint(String token) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private TokenPayload readPayload(String encodedPayload) throws Exception {
        return json.readValue(Base64.getUrlDecoder().decode(encodedPayload), TokenPayload.class);
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

    private record TokenPayload(String sub, String iss, String aud, long exp) { }
}
