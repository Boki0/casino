package com.boki0.casino.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "NSedStA/GLFKtUz3SOIuyrchg6bzht1Uk+fNQ5unfNv2bYGwGbPPg7s7hWw2P98i47GJmegCxNCthEPFRQlhSA==";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void validateAndExtractUser_shouldExtractUserContextFromValidToken() {
        String authUserId = UUID.randomUUID().toString();
        String token = createToken(authUserId, "player@example.com", "USER", Instant.now().plusSeconds(900));

        AuthenticatedUser authenticatedUser = jwtService.validateAndExtractUser(token);

        assertEquals(authUserId, authenticatedUser.authUserId());
        assertEquals("player@example.com", authenticatedUser.email());
        assertEquals("USER", authenticatedUser.role());
    }

    @Test
    void validateAndExtractUser_shouldRejectExpiredToken() {
        String token = createToken(UUID.randomUUID().toString(), "player@example.com", "USER", Instant.now().minusSeconds(60));

        assertThrows(RuntimeException.class, () -> jwtService.validateAndExtractUser(token));
    }

    private String createToken(String authUserId, String email, String role, Instant expiresAt) {
        SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim("userId", authUserId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }
}
