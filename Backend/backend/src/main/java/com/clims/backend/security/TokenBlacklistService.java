package com.clims.backend.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService implements TokenBlacklist {
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        if (jti == null) return;
        blacklist.put(jti, Instant.now().plusSeconds(ttlSeconds).toEpochMilli());
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        Long exp = blacklist.get(jti);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }
}