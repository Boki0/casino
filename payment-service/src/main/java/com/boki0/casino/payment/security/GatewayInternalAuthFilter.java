package com.boki0.casino.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayInternalAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";
    private static final String PROTECTED_PAYMENTS_PATH = "/payments";
    private static final String PROTECTED_PAYMENTS_PATH_PREFIX = "/payments/";

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
        if (!isProtectedPaymentsPath(request)) {
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

    private boolean isProtectedPaymentsPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri.equals(PROTECTED_PAYMENTS_PATH) || requestUri.startsWith(PROTECTED_PAYMENTS_PATH_PREFIX);
    }
}
