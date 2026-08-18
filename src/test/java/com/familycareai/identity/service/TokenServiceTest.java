package com.familycareai.identity.service;

import com.familycareai.audit.service.AuditLogService;
import com.familycareai.common.exception.TokenReuseException;
import com.familycareai.identity.entity.RefreshToken;
import com.familycareai.identity.entity.User;
import com.familycareai.identity.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TokenService tokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "refreshExpirationDays", 7L);
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@familycare.ai")
                .build();
    }

    @Test
    @DisplayName("Should hash token using SHA-256 deterministically")
    void testHashToken() {
        String rawToken = "sample-raw-refresh-token-123";
        String hash1 = tokenService.hashToken(rawToken);
        String hash2 = tokenService.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should create refresh token and save entity to repository")
    void testCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String rawToken = tokenService.createRefreshToken(user, "Test Device", "127.0.0.1");

        assertNotNull(rawToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should rotate refresh token when valid token is presented")
    void testRotateRefreshTokenSuccess() {
        String rawToken = "valid-refresh-token";
        String tokenHash = tokenService.hashToken(rawToken);

        RefreshToken activeToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(activeToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        TokenService.TokenPairResult result = tokenService.verifyAndRotateRefreshToken(rawToken, "127.0.0.1", "Test Device");

        assertNotNull(result);
        assertNotNull(result.getNewRawRefreshToken());
        assertNotEquals(rawToken, result.getNewRawRefreshToken());
        assertNotNull(activeToken.getRevokedAt());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should detect token reuse and revoke all user sessions")
    void testTokenReuseDetection() {
        String rawToken = "revoked-refresh-token";
        String tokenHash = tokenService.hashToken(rawToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revokedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThrows(TokenReuseException.class, () ->
                tokenService.verifyAndRotateRefreshToken(rawToken, "127.0.0.1", "Test Device")
        );

        verify(refreshTokenRepository, times(1)).revokeAllActiveTokensByUserId(eq(user.getId()), any(Instant.class));
    }
}
