package com.boki0.casino.wallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GatewayInternalAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";
    private static final String PROTECTED_WALLET_PATH = "/wallet";
    private static final String PROTECTED_WALLET_PATH_PREFIX = "/wallet/";

    private final String internalGatewaySecret;

    public GatewayInternalAuthFilter(@Value("${internal.gateway.secret}") String internalGatewaySecret) {
        this.internalGatewaySecret = internalGatewaySecret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isProtectedWalletPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedSecret = request.getHeader(HEADER_INTERNAL_GATEWAY_SECRET);
        if (!internalGatewaySecret.equals(providedSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized internal gateway request\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedWalletPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri.equals(PROTECTED_WALLET_PATH) || requestUri.startsWith(PROTECTED_WALLET_PATH_PREFIX);
    }
}
