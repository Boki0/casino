package com.boki0.casino.game.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTokenServiceTest {

    private final GameSessionTokenService tokenService = new GameSessionTokenService();

    @Test
    void shouldGenerateNonBlankUrlSafeTokenWithoutPadding() {
        String token = tokenService.generateToken();

        assertFalse(token.isBlank());
        assertTrue(token.matches("[A-Za-z0-9_-]+"));
        assertFalse(token.contains("="));
        assertFalse(token.contains(" "));
    }

    @Test
    void shouldGenerateDifferentTokens() {
        assertNotEquals(tokenService.generateToken(), tokenService.generateToken());
    }

    @Test
    void shouldHashTokenAsDeterministicLowercaseSha256Hex() {
        String firstHash = tokenService.hashToken("session-token");
        String secondHash = tokenService.hashToken("session-token");

        assertEquals(firstHash, secondHash);
        assertEquals(64, firstHash.length());
        assertTrue(firstHash.matches("[0-9a-f]{64}"));
    }

    @Test
    void shouldProduceDifferentHashesForDifferentTokens() {
        assertNotEquals(
                tokenService.hashToken("first-token"),
                tokenService.hashToken("second-token")
        );
    }

    @Test
    void shouldRejectNullAndBlankTokens() {
        assertThrows(IllegalArgumentException.class, () -> tokenService.hashToken(null));
        assertThrows(IllegalArgumentException.class, () -> tokenService.hashToken(""));
        assertThrows(IllegalArgumentException.class, () -> tokenService.hashToken("   "));
    }
}
