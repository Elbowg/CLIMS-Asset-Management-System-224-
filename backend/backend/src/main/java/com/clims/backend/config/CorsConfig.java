package com.clims.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsProp;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethodsProp;

    @Value("${app.cors.allowed-headers:Authorization,Content-Type}")
    private String allowedHeadersProp;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = splitAndTrim(allowedOriginsProp);
        if (origins.isEmpty()) {
            // Fallback for local dev if the property/environment isn't set
            origins = List.of("http://localhost:5173", "http://127.0.0.1:5173");
        } else {
            // Use exact origins for clarity with credentials
        }

        // Prefer explicit allowed origins over patterns when using credentials
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(splitAndTrim(allowedMethodsProp));
        config.setAllowedHeaders(splitAndTrim(allowedHeadersProp));
        config.setAllowCredentials(true);
        // Expose headers that clients may need
        config.setExposedHeaders(List.of("Content-Disposition", "X-Report-Limited", "X-Report-Limit"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

    private static List<String> splitAndTrim(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
