package com.clims.backend.security;

import com.clims.backend.config.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {
    JwtUtil jwtUtil;
    JwtProperties props;
    JwtKeyProvider keyProvider;

    @BeforeEach
    void setup() {
        props = new JwtProperties();
        // legacy single secret path used if secrets not set
        props.setSecret("01234567890123456789012345678901");
        props.setAccessExpiration(5000L); // 5s
        props.setRefreshExpiration(10000L); // 10s
        keyProvider = new JwtKeyProvider();
        keyProvider.setSecret(props.getSecret());
        jwtUtil = new JwtUtil(props, keyProvider);
    }

    @Test
    void generatesAccessTokenWithRolesAndTypeClaim() {
        User principal = new User("alice", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var auth = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        String token = jwtUtil.generateAccessToken(auth);
        assertThat(jwtUtil.validateJwtToken(token)).isTrue();
        assertThat(jwtUtil.isAccessToken(token)).isTrue();
        assertThat(jwtUtil.getUsername(token)).isEqualTo("alice");
    }

    @Test
    void generatesRefreshTokenWithTypeClaim() {
        String token = jwtUtil.generateRefreshToken("bob");
        assertThat(jwtUtil.validateJwtToken(token)).isTrue();
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
        assertThat(jwtUtil.getUsername(token)).isEqualTo("bob");
    }

    @Test
    @DisplayName("Token signed with old secret still validates after rotation when listed in secondary position")
    void oldSecretStillValidAfterRotation() {
        // 1. Issue token with original single secret configuration
        String originalAccess = jwtUtil.generateAccessTokenFromUsername("carol");
        assertThat(jwtUtil.validateJwtToken(originalAccess)).isTrue();

        // 2. Simulate rotation: move old secret to second, introduce new primary
        String newPrimary = "ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCD"; // 32 chars
        keyProvider.setSecrets(newPrimary + "," + props.getSecret());

        // 3. New JwtUtil (properties unchanged, key provider now multi-secret) should still validate old token
        assertThat(jwtUtil.validateJwtToken(originalAccess)).as("Old token should remain valid during overlap window").isTrue();

        // 4. Tokens issued now should be signed by new primary and still parse
        String rotatedAccess = jwtUtil.generateAccessTokenFromUsername("carol");
        assertThat(jwtUtil.validateJwtToken(rotatedAccess)).isTrue();
        assertThat(jwtUtil.getUsername(rotatedAccess)).isEqualTo("carol");
    }
}
