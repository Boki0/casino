package com.boki0.casino.user.config;

import com.boki0.casino.user.security.GatewayInternalAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final GatewayInternalAuthFilter gatewayInternalAuthFilter;

    public SecurityConfig(GatewayInternalAuthFilter gatewayInternalAuthFilter) {
        this.gatewayInternalAuthFilter = gatewayInternalAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/internal/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/me").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/users/me").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(gatewayInternalAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
