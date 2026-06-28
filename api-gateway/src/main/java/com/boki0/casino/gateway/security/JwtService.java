package com.boki0.casino.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser validateAndExtractUser(String token) {
        Claims claims = extractAllClaims(token);
        String authUserId = claims.get(USER_ID_CLAIM, String.class);
        String email = claims.getSubject();
        String role = claims.get(ROLE_CLAIM, String.class);

        if (authUserId == null || authUserId.isBlank()) {
            throw new JwtException("Missing userId claim");
        }

        return new AuthenticatedUser(authUserId, email, role);
    }

    Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
