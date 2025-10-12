package com.clims.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Insecure security configuration for LOCAL DEVELOPMENT ONLY.
 * <p>
 * This configuration permits all requests without authentication when the "insecure" profile is active.
 * It is intended ONLY for rapid local development and testing.
 * <p>
 * WARNING: DO NOT enable the "insecure" profile in production, CI/CD, or any shared environment.
 * <p>
 * Usage:
 * - Maven: ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,insecure
 * - JAR: java -jar backend.jar --spring.profiles.active=local,insecure
 */
@Configuration
@Profile("insecure")
public class InsecureSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(InsecureSecurityConfig.class);

    public InsecureSecurityConfig() {
        logger.warn("⚠️  INSECURE PROFILE ACTIVE - ALL REQUESTS PERMITTED WITHOUT AUTHENTICATION");
        logger.warn("⚠️  THIS IS FOR LOCAL DEVELOPMENT ONLY - DO NOT USE IN PRODUCTION");
    }

    @Bean
    @Order(1) // Higher precedence than default SecurityConfig
    public SecurityFilterChain insecureFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow common localhost ports and file:// origin
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5500",
            "http://localhost:8080",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5500",
            "http://127.0.0.1:8080",
            "null" // file:// protocol
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
