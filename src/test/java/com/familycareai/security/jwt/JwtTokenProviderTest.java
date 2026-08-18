package com.familycareai.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMinutes = 15;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMinutes);
    }

    @Test
    @DisplayName("Should generate a valid JWT access token with subject, email, and roles")
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String email = "test.patient@familycare.ai";
        List<String> roles = List.of("ROLE_PATIENT");

        String token = jwtTokenProvider.generateAccessToken(userId, email, roles);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    @DisplayName("Should reject invalid or tampered JWT token")
    void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalidPayload.invalidSignature";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }
}
