package com.payrecon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the Angular dev server (a different origin) to call this API
 * during local development. In production the frontend is served by nginx
 * proxying to this API under the same origin (see Phase 6 docker-compose),
 * so this CORS config only matters for `ng serve` against a locally running
 * backend.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${payrecon.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
