package com.womansday.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-32-characters-long",
                3600000L,   // 1 hour
                604800000L  // 7 days
        );
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        String token = tokenProvider.generateAccessToken(1L, "testuser", "USER");

        assertTrue(tokenProvider.validateToken(token));
        assertEquals(1L, tokenProvider.getUserId(token));
        assertEquals("USER", tokenProvider.getRole(token));
        assertEquals("access", tokenProvider.getTokenType(token));
    }

    @Test
    void generateRefreshToken_shouldCreateValidToken() {
        String token = tokenProvider.generateRefreshToken(1L);

        assertTrue(tokenProvider.validateToken(token));
        assertEquals(1L, tokenProvider.getUserId(token));
        assertEquals("refresh", tokenProvider.getTokenType(token));
        assertNotNull(tokenProvider.getJti(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void generateRefreshToken_shouldHaveUniqueJti() {
        String token1 = tokenProvider.generateRefreshToken(1L);
        String token2 = tokenProvider.generateRefreshToken(1L);

        assertNotEquals(tokenProvider.getJti(token1), tokenProvider.getJti(token2));
    }

    @Test
    void getExpiration_shouldReturnFutureDate() {
        String token = tokenProvider.generateAccessToken(1L, "testuser", "USER");
        assertNotNull(tokenProvider.getExpiration(token));
        assertTrue(tokenProvider.getExpiration(token).getTime() > System.currentTimeMillis());
    }

    @Test
    void getAccessExpirationSeconds_shouldReturnCorrectValue() {
        assertEquals(3600L, tokenProvider.getAccessExpirationSeconds());
    }
}
