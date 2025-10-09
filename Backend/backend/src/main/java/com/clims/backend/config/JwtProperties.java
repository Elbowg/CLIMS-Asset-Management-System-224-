package com.clims.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.annotation.PostConstruct;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** Secret key for signing JWTs */
    @NotBlank
    @Size(min = 32, message = "JWT secret must be at least 32 characters for HS256 security")
    private String secret = "defaultSecretKeyForDevelopmentPurposesOnlyChangeInProduction";
    /** Access token TTL in milliseconds */
    @Min(value = 1000, message = "Access token expiration must be >= 1000 ms")
    private long accessExpiration = 900_000; // 15m
    /** Refresh token TTL in milliseconds */
    @Min(value = 60_000, message = "Refresh token expiration must be >= 60000 ms")
    private long refreshExpiration = 7 * 24 * 60 * 60 * 1000L; // 7d

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessExpiration() { return accessExpiration; }
    public void setAccessExpiration(long accessExpiration) { this.accessExpiration = accessExpiration; }
    public long getRefreshExpiration() { return refreshExpiration; }
    public void setRefreshExpiration(long refreshExpiration) { this.refreshExpiration = refreshExpiration; }

    @PostConstruct
    void validateLogicalConsistency() {
        if (refreshExpiration <= accessExpiration) {
            throw new IllegalStateException("Refresh expiration must be greater than access expiration");
        }
    }
}
