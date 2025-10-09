package com.clims.backend.security;

import io.jsonwebtoken.*;
import com.clims.backend.config.JwtProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final JwtProperties properties;
    private final JwtKeyProvider keyProvider;

    public JwtUtil(JwtProperties properties, JwtKeyProvider keyProvider) {
        this.properties = properties;
        this.keyProvider = keyProvider;
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
    return buildToken(principal.getUsername(), JwtTokenType.ACCESS, properties.getAccessExpiration(), principal);
    }

    public String generateAccessTokenFromUsername(String username) {
    return buildToken(username, JwtTokenType.ACCESS, properties.getAccessExpiration(), null);
    }

    public String generateRefreshToken(String username) {
    return buildToken(username, JwtTokenType.REFRESH, properties.getRefreshExpiration(), null);
    }

    private String buildToken(String username, JwtTokenType type, long ttl, UserDetails principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttl);
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .claim("typ", type.name())
                .signWith(keyProvider.signingKey(), SignatureAlgorithm.HS256);
        if (principal != null && type == JwtTokenType.ACCESS) {
            builder.claim("roles", principal.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        }
        return builder.compact();
    }

    public Claims parseClaims(String token) {
        // Attempt verification against all configured keys (supports rotation window)
        var keys = keyProvider.verificationKeys();
        JwtException last = null;
        for (SecretKey k : keys) {
            try {
                return Jwts.parserBuilder().setSigningKey(k).build().parseClaimsJws(token).getBody();
            } catch (JwtException ex) {
                last = ex; // try next
            }
        }
        if (last != null) throw last;
        throw new JwtException("No verification keys configured");
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isAccessToken(String token) { return tokenType(token) == JwtTokenType.ACCESS; }
    public boolean isRefreshToken(String token) { return tokenType(token) == JwtTokenType.REFRESH; }

    private JwtTokenType tokenType(String token) {
        Object val = parseClaims(token).get("typ");
        if (val == null) return JwtTokenType.ACCESS; // backwards compatibility fallback
        try { return JwtTokenType.valueOf(val.toString()); } catch (Exception e) { return JwtTokenType.ACCESS; }
    }

    public boolean validateJwtToken(String authToken) {
        try {
            parseClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }

    public long getAccessExpirationSeconds() { return properties.getAccessExpiration() / 1000; }
    public long getRefreshExpirationSeconds() { return properties.getRefreshExpiration() / 1000; }

    public String getJti(String token) { return parseClaims(token).getId(); }
}