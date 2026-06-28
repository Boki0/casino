package com.boki0.casino.gateway.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAuthenticationFilterTest {

    private final GatewayAuthenticationFilter filter = new GatewayAuthenticationFilter(new JwtService(
            "NSedStA/GLFKtUz3SOIuyrchg6bzht1Uk+fNQ5unfNv2bYGwGbPPg7s7hWw2P98i47GJmegCxNCthEPFRQlhSA=="
    ));

    @Test
    void isPublicPath_shouldReturnTrueForAuthPublicPaths() {
        assertTrue(filter.isPublicPath("/api/auth/register"));
        assertTrue(filter.isPublicPath("/api/auth/login"));
        assertTrue(filter.isPublicPath("/api/auth/refresh"));
        assertTrue(filter.isPublicPath("/api/auth/logout"));
    }

    @Test
    void isPublicPath_shouldReturnFalseForProtectedPaths() {
        assertFalse(filter.isPublicPath("/api/users/me"));
        assertFalse(filter.isPublicPath("/api/auth/me"));
        assertFalse(filter.isPublicPath("/api/games"));
    }
}
