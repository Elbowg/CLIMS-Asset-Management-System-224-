package com.clims.backend.security;

/**
 * Abstraction for token blacklist so implementations (in-memory, Redis, DB) can be swapped.
 */
public interface TokenBlacklist {
    void blacklist(String jti, long ttlSeconds);
    boolean isBlacklisted(String jti);
}
