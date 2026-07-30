package com.boki0.casino.game.config;

import com.boki0.casino.game.security.GatewayLaunchAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final GatewayLaunchAuthenticationInterceptor launchAuthenticationInterceptor;

    public WebConfig(GatewayLaunchAuthenticationInterceptor launchAuthenticationInterceptor) {
        this.launchAuthenticationInterceptor = launchAuthenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(launchAuthenticationInterceptor)
                .addPathPatterns("/games/*/launch");
    }
}
