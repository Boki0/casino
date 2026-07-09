package com.boki0.casino.payment.config;

import com.boki0.casino.payment.security.GatewayInternalAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/payments/**").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(gatewayInternalAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
