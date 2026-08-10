package com.boki0.casino.gateway.security;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";
    private static final String HEADER_AUTH_USER_ROLE = "X-Auth-User-Role";
    private static final String HEADER_AUTH_USER_EMAIL = "X-Auth-User-Email";
    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/payments/webhooks/stripe"
    );

    private final JwtService jwtService;
    private final String internalGatewaySecret;

    public GatewayAuthenticationFilter(
            JwtService jwtService,
            @Value("${internal.gateway.secret}") String internalGatewaySecret
    ) {
        this.jwtService = jwtService;
        this.internalGatewaySecret = internalGatewaySecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = removeClientAuthHeaders(exchange.getRequest());
        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(sanitizedRequest)
                .build();

        String path = sanitizedRequest.getURI().getPath();
        if (isPublicPath(path, sanitizedRequest.getMethod())) {
            return chain.filter(sanitizedExchange);
        }

        String authorizationHeader = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return unauthorized(sanitizedExchange, "Missing Authorization header");
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(sanitizedExchange, "Invalid Authorization header");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            return unauthorized(sanitizedExchange, "Missing Bearer token");
        }

        try {
            AuthenticatedUser authenticatedUser = jwtService.validateAndExtractUser(token);
            LOGGER.info("Authenticated request path={} authUserId={}", path, authenticatedUser.authUserId());

            ServerHttpRequest authenticatedRequest = sanitizedRequest.mutate()
                    .header(HEADER_AUTH_USER_ID, authenticatedUser.authUserId())
                    .header(HEADER_INTERNAL_GATEWAY_SECRET, internalGatewaySecret)
                    .headers(headers -> {
                        if (authenticatedUser.role() != null && !authenticatedUser.role().isBlank()) {
                            headers.set(HEADER_AUTH_USER_ROLE, authenticatedUser.role());
                        }
                        if (authenticatedUser.email() != null && !authenticatedUser.email().isBlank()) {
                            headers.set(HEADER_AUTH_USER_EMAIL, authenticatedUser.email());
                        }
                    })
                    .build();

            return chain.filter(sanitizedExchange.mutate()
                    .request(authenticatedRequest)
                    .build());
        } catch (JwtException | IllegalArgumentException exception) {
            LOGGER.info("Rejected request path={} reason={}", path, exception.getClass().getSimpleName());
            return unauthorized(sanitizedExchange, "Invalid or expired token");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    boolean isPublicPath(String path, HttpMethod method) {
        return PUBLIC_PATHS.contains(path)
                || (HttpMethod.POST.equals(method)
                && (path.equals("/api/provider-wallet/authenticate")
                || path.equals("/api/provider-wallet/bet")
                || path.equals("/api/provider-wallet/result")))
                || (HttpMethod.GET.equals(method)
                && (path.equals("/api/games") || path.startsWith("/api/games/")));
    }

    private ServerHttpRequest removeClientAuthHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(HEADER_AUTH_USER_ID);
                    headers.remove(HEADER_AUTH_USER_ROLE);
                    headers.remove(HEADER_AUTH_USER_EMAIL);
                    headers.remove(HEADER_INTERNAL_GATEWAY_SECRET);
                })
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
