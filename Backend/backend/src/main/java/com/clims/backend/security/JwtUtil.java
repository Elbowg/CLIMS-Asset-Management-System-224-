package com.clims.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:defaultSecretKeyForDevelopmentPurposesOnlyChangeInProduction}")
    private String jwtSecret;

    @Value("${jwt.access-expiration:900000}") // 15 minutes
    private long accessExpiration;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        return buildToken(principal.getUsername(), JwtTokenType.ACCESS, accessExpiration, principal);
    }

    public String generateAccessTokenFromUsername(String username) {
        return buildToken(username, JwtTokenType.ACCESS, accessExpiration, null);
    }

    public String generateRefreshToken(String username) {
        return buildToken(username, JwtTokenType.REFRESH, refreshExpiration, null);
    }

    private String buildToken(String username, JwtTokenType type, long ttl, UserDetails principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttl);
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("typ", type.name())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);
        if (principal != null && type == JwtTokenType.ACCESS) {
            builder.claim("roles", principal.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        }
        return builder.compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
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

    public long getAccessExpirationSeconds() { return accessExpiration / 1000; }
    public long getRefreshExpirationSeconds() { return refreshExpiration / 1000; }
}