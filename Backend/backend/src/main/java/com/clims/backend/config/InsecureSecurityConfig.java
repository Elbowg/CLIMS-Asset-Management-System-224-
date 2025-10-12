package com.clims.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Insecure security configuration for local development only.
 * Permits all requests without authentication.
 * 
 * WARNING: Only activate with -Dspring.profiles.active=local,insecure
 * Never use this in production or any shared environment!
 */
@Configuration
@EnableWebSecurity
@Profile("insecure")
@Order(1) // Higher precedence than default SecurityConfig
public class InsecureSecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(InsecureSecurityConfig.class);
    
    public InsecureSecurityConfig() {
        logger.warn("╔═══════════════════════════════════════════════════════════════════╗");
        logger.warn("║  ⚠️  INSECURE MODE ACTIVE - ALL ENDPOINTS PERMIT ALL REQUESTS  ⚠️  ║");
        logger.warn("║                                                                   ║");
        logger.warn("║  Authentication is DISABLED. This should ONLY be used for        ║");
        logger.warn("║  local development on 127.0.0.1. Never use in production!        ║");
        logger.warn("╚═══════════════════════════════════════════════════════════════════╝");
    }
    
    @Bean
    public SecurityFilterChain insecureFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring INSECURE security filter chain (permits all)");
        
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(insecureCorsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource insecureCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow common localhost origins for development
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5000",
            "http://localhost:5500",
            "http://localhost:8000",
            "http://localhost:8080",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5000",
            "http://127.0.0.1:5500",
            "http://127.0.0.1:8000",
            "http://127.0.0.1:8080",
            "null" // For file:// protocol during development
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
