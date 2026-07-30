package com.boki0.casino.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAuthenticationFilterTest {

    private static final String SECRET = "NSedStA/GLFKtUz3SOIuyrchg6bzht1Uk+fNQ5unfNv2bYGwGbPPg7s7hWw2P98i47GJmegCxNCthEPFRQlhSA==";
    private static final String INTERNAL_SECRET = "local-gateway-secret-change-me";

    private final GatewayAuthenticationFilter filter = new GatewayAuthenticationFilter(
            new JwtService(SECRET),
            INTERNAL_SECRET
    );

    @Test
    void isPublicPath_shouldReturnTrueForAuthPublicPaths() {
        assertTrue(filter.isPublicPath("/api/auth/register", HttpMethod.POST));
        assertTrue(filter.isPublicPath("/api/auth/login", HttpMethod.POST));
        assertTrue(filter.isPublicPath("/api/auth/refresh", HttpMethod.POST));
        assertTrue(filter.isPublicPath("/api/auth/logout", HttpMethod.POST));
        assertTrue(filter.isPublicPath("/api/games", HttpMethod.GET));
        assertTrue(filter.isPublicPath("/api/games/123", HttpMethod.GET));
        assertTrue(filter.isPublicPath("/api/provider-wallet/authenticate", HttpMethod.POST));
    }

    @Test
    void isPublicPath_shouldReturnFalseForProtectedPaths() {
        assertFalse(filter.isPublicPath("/api/users/me", HttpMethod.GET));
        assertFalse(filter.isPublicPath("/api/auth/me", HttpMethod.GET));
        assertFalse(filter.isPublicPath("/api/games/123/launch", HttpMethod.POST));
        assertFalse(filter.isPublicPath("/api/provider-wallet/bet", HttpMethod.POST));
    }

    @Test
    void filter_shouldRejectAnonymousGameLaunch() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/games/123/launch")
        );

        filter.filter(exchange, ignored -> Mono.error(new AssertionError("chain must not be called"))).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_shouldRemoveClientInternalSecretAndAddConfiguredSecret() {
        String authUserId = UUID.randomUUID().toString();
        String token = createToken(authUserId, "player@example.com", "USER", Instant.now().plusSeconds(900));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Internal-Gateway-Secret", "client-secret")
                .header("X-Auth-User-Id", "client-user-id"));
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        GatewayFilterChain chain = nextExchange -> {
            forwardedRequest.set(nextExchange.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals(INTERNAL_SECRET, forwardedRequest.get().getHeaders().getFirst("X-Internal-Gateway-Secret"));
        assertEquals(authUserId, forwardedRequest.get().getHeaders().getFirst("X-Auth-User-Id"));
        assertEquals("USER", forwardedRequest.get().getHeaders().getFirst("X-Auth-User-Role"));
        assertEquals("player@example.com", forwardedRequest.get().getHeaders().getFirst("X-Auth-User-Email"));
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
