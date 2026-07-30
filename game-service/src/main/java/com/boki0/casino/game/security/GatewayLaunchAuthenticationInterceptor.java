package com.boki0.casino.game.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
public class GatewayLaunchAuthenticationInterceptor implements HandlerInterceptor {

    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";
    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";

    private final byte[] internalGatewaySecret;

    public GatewayLaunchAuthenticationInterceptor(
            @Value("${internal.gateway.secret}") String internalGatewaySecret
    ) {
        this.internalGatewaySecret = internalGatewaySecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if (!isLaunchRequest(request)) {
            return true;
        }

        String providedSecret = request.getHeader(HEADER_INTERNAL_GATEWAY_SECRET);
        if (!hasValidGatewaySecret(providedSecret) || !hasValidPlayerId(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized game launch request\"}");
            return false;
        }

        return true;
    }

    private boolean isLaunchRequest(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }

        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith("/games/") || !requestUri.endsWith("/launch")) {
            return false;
        }

        String gameIdSegment = requestUri.substring("/games/".length(), requestUri.length() - "/launch".length());
        return !gameIdSegment.isBlank() && gameIdSegment.indexOf('/') == -1;
    }

    private boolean hasValidGatewaySecret(String providedSecret) {
        return providedSecret != null && MessageDigest.isEqual(
                internalGatewaySecret,
                providedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean hasValidPlayerId(HttpServletRequest request) {
        String playerId = request.getHeader(HEADER_AUTH_USER_ID);
        if (playerId == null || playerId.isBlank()) {
            return false;
        }

        try {
            UUID.fromString(playerId);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
