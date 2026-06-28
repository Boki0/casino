package com.boki0.casino.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayInternalAuthFilterTest {

    private static final String INTERNAL_SECRET = "local-gateway-secret-change-me";

    private final GatewayInternalAuthFilter filter = new GatewayInternalAuthFilter(INTERNAL_SECRET);

    @Test
    void doFilterInternal_shouldRejectUsersRequestWithoutInternalSecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("X-Auth-User-Id", "2bbd8407-e2ab-4a60-8988-7ba82dd111b3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noOpFilterChain());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void doFilterInternal_shouldRejectUsersRequestWithInvalidInternalSecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("X-Internal-Gateway-Secret", "wrong-secret");
        request.addHeader("X-Auth-User-Id", "2bbd8407-e2ab-4a60-8988-7ba82dd111b3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noOpFilterChain());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void doFilterInternal_shouldAllowUsersRequestWithValidInternalSecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("X-Internal-Gateway-Secret", INTERNAL_SECRET);
        request.addHeader("X-Auth-User-Id", "2bbd8407-e2ab-4a60-8988-7ba82dd111b3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CountingFilterChain filterChain = new CountingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(1, filterChain.callCount);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void doFilterInternal_shouldNotBlockInternalUsersEndpoint() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CountingFilterChain filterChain = new CountingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(1, filterChain.callCount);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    private FilterChain noOpFilterChain() {
        return (request, response) -> {
        };
    }

    private static class CountingFilterChain implements FilterChain {
        private int callCount;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            callCount++;
        }
    }

}
