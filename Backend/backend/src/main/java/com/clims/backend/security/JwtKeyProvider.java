package com.clims.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Supports JWT key rotation: configure comma-separated secrets via `jwt.secrets`.
 * First secret is used for signing; all provided secrets are accepted for verification.
 * Falls back to single legacy property `jwt.secret` if `jwt.secrets` absent.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtKeyProvider {

    /** Comma separated secrets (new preferred property). */
    private String secrets;
    /** Legacy single secret. */
    private String secret;

    public void setSecrets(String secrets) { this.secrets = secrets; }
    public void setSecret(String secret) { this.secret = secret; }

    public SecretKey signingKey() {
        return Keys.hmacShaKeyFor(primaryRaw().getBytes(StandardCharsets.UTF_8));
    }

    public List<SecretKey> verificationKeys() {
        List<String> raw = allRaw();
        List<SecretKey> keys = new ArrayList<>(raw.size());
        for (String s : raw) {
            keys.add(Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8)));
        }
        return Collections.unmodifiableList(keys);
    }

    private String primaryRaw() {
        if (StringUtils.hasText(secrets)) {
            return secrets.split(",")[0].trim();
        }
        return secret; // legacy
    }

    private List<String> allRaw() {
        List<String> list = new ArrayList<>();
        if (StringUtils.hasText(secrets)) {
            for (String part : secrets.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) list.add(t);
            }
        } else if (StringUtils.hasText(secret)) {
            list.add(secret);
        }
        return list;
    }
}
