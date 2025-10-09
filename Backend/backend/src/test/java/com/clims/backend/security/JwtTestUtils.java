package com.clims.backend.security;

import com.clims.backend.config.JwtProperties;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

public class JwtTestUtils {
    public static JwtUtil jwtUtil(String secret, long accessMs, long refreshMs) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setAccessExpiration(accessMs);
        props.setRefreshExpiration(refreshMs);
        JwtKeyProvider keyProvider = new JwtKeyProvider();
        keyProvider.setSecret(secret);
        return new JwtUtil(props, keyProvider);
    }

    public static Authentication auth(String username, String... roles) {
        var authorities = roles == null ? List.<SimpleGrantedAuthority>of() :
                java.util.Arrays.stream(roles).map(r -> new SimpleGrantedAuthority(r)).toList();
        User principal = new User(username, "password", authorities);
        return new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
    }
}
