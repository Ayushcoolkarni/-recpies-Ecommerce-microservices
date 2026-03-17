package com.example.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // Decode the same hex secret used by user-service
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates the token signature and expiry.
     * Returns true if the token is well-formed and not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    /** Extracts the subject (userId) from the token. */
    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    /** Extracts the role claim from the token. */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /** Extracts the email claim from the token. */
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    // ── private ──────────────────────────────────────────────────

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
