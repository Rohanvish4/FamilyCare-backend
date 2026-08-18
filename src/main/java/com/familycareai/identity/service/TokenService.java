package com.familycareai.identity.service;

import com.familycareai.audit.entity.AuditAction;
import com.familycareai.audit.entity.AuditStatus;
import com.familycareai.audit.service.AuditLogService;
import com.familycareai.common.exception.InvalidTokenException;
import com.familycareai.common.exception.TokenReuseException;
import com.familycareai.identity.entity.RefreshToken;
import com.familycareai.identity.entity.User;
import com.familycareai.identity.repository.RefreshTokenRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;

    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    @Getter
    public static class TokenPairResult {
        private final String newRawRefreshToken;
        private final User user;

        public TokenPairResult(String newRawRefreshToken, User user) {
            this.newRawRefreshToken = newRawRefreshToken;
            this.user = user;
        }
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    public String createRefreshToken(User user, String deviceInfo, String ipAddress) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshExpirationDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("Created refresh token hash: {} for user: {}", tokenHash, user.getId());
        return rawToken;
    }

    @Transactional
    public TokenPairResult verifyAndRotateRefreshToken(String rawRefreshToken, String ipAddress, String deviceInfo) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        User user = storedToken.getUser();

        // Check for Token Reuse (attempting to use a revoked token)
        if (storedToken.isRevoked()) {
            log.error("CRITICAL SECURITY VIOLATION: Revoked refresh token reuse attempt detected for user: {}", user.getId());

            // Immediately revoke all active sessions for that user
            refreshTokenRepository.revokeAllActiveTokensByUserId(user.getId(), Instant.now());

            auditLogService.logAction(
                    user.getId(),
                    AuditAction.TOKEN_REFRESHED,
                    ipAddress,
                    deviceInfo,
                    AuditStatus.FAILURE,
                    Map.of("reason", "Revoked refresh token reuse detected - All user sessions revoked", "tokenHash", tokenHash)
            );

            throw new TokenReuseException("Security violation: Revoked refresh token reuse detected. All active sessions have been invalidated.");
        }

        // Check for Expiration
        if (storedToken.isExpired()) {
            log.warn("Refresh token expired for user: {}", user.getId());
            throw new InvalidTokenException("Refresh token has expired. Please login again.");
        }

        // Perform Refresh Token Rotation
        String newRawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String newTokenHash = hashToken(newRawRefreshToken);

        Instant now = Instant.now();
        Instant newExpiresAt = now.plus(refreshExpirationDays, ChronoUnit.DAYS);

        storedToken.setRevokedAt(now);
        storedToken.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(storedToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .deviceInfo(deviceInfo != null ? deviceInfo : storedToken.getDeviceInfo())
                .ipAddress(ipAddress != null ? ipAddress : storedToken.getIpAddress())
                .expiresAt(newExpiresAt)
                .createdAt(now)
                .build();

        refreshTokenRepository.save(newRefreshToken);
        log.info("Successfully rotated refresh token for user: {}", user.getId());

        return new TokenPairResult(newRawRefreshToken, user);
    }

    @Transactional
    public void revokeToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
                log.debug("Revoked refresh token hash: {}", tokenHash);
            }
        });
    }
}
