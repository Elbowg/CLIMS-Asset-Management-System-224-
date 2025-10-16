package com.clims.backend.services;

import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.RefreshToken;
import com.clims.backend.repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.refresh.expiration-seconds:604800}") // default 7 days
    private long refreshTtl;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken createRefreshToken(AppUser user) {
        // remove existing tokens for user
        refreshTokenRepository.deleteByUser(user);
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(refreshTtl));
        return refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token).filter(t -> !t.isRevoked() && t.getExpiryDate().isAfter(Instant.now()));
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeForUser(AppUser user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
