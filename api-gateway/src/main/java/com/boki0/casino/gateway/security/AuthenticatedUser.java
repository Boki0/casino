package com.boki0.casino.gateway.security;

public record AuthenticatedUser(
        String authUserId,
        String email,
        String role
) {
}
