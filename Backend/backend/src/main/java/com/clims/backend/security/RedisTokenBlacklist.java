package com.clims.backend.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed implementation of {@link TokenBlacklist}. Enabled when spring.data.redis.host is present
 * and a {@link StringRedisTemplate} bean is on the classpath. Marks entries with an expiry matching the
 * remaining lifetime of the token (ttlSeconds).
 */
@Service
@Primary
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
@ConditionalOnMissingBean(name = "disabledRedisTokenBlacklist") // safety hook if user wants to force-disable
public class RedisTokenBlacklist implements TokenBlacklist {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "jwt:blacklist:";

    public RedisTokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        if (jti == null) return;
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        return redisTemplate.hasKey(KEY_PREFIX + jti);
    }
}
