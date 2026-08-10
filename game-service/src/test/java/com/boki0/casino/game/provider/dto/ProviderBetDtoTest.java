package com.boki0.casino.game.provider.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderBetDtoTest {

    private static final UUID PLAYER_ID =
            UUID.fromString("6a71d2e2-a81a-490a-adf9-d5896a20c483");
    private static final UUID TRANSACTION_ID =
            UUID.fromString("7035a82f-146a-42a0-ab95-07d867854ada");

    private static ObjectMapper objectMapper;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validProviderBetJsonDeserializesWithExactFieldNamesAndMoneyType() throws Exception {
        ProviderBetRequest request = objectMapper.readValue("""
                {
                  "userId": "6a71d2e2-a81a-490a-adf9-d5896a20c483",
                  "gameId": "NOVA_SEVEN",
                  "roundId": "provider-round-id",
                  "amount": 1.00,
                  "reference": "provider-bet-reference",
                  "providerId": "NOVA_REELS",
                  "timestamp": 1780000000000,
                  "roundDetails": "spin",
                  "platform": "WEB",
                  "language": "en",
                  "token": "operator-generated-token",
                  "ipAddress": "127.0.0.1"
                }
                """, ProviderBetRequest.class);

        assertEquals(PLAYER_ID, request.userId());
        assertEquals("NOVA_SEVEN", request.gameId());
        assertEquals("provider-round-id", request.roundId());
        assertEquals(new BigDecimal("1.00"), request.amount());
        assertEquals("provider-bet-reference", request.reference());
        assertEquals("NOVA_REELS", request.providerId());
        assertEquals(1780000000000L, request.timestamp());
        assertEquals("spin", request.roundDetails());
        assertEquals("WEB", request.platform());
        assertEquals("en", request.language());
        assertEquals("operator-generated-token", request.token());
        assertEquals("127.0.0.1", request.ipAddress());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void optionalInformationalFieldsMayBeOmitted() throws Exception {
        ProviderBetRequest request = objectMapper.readValue("""
                {
                  "userId": "6a71d2e2-a81a-490a-adf9-d5896a20c483",
                  "gameId": "NOVA_SEVEN",
                  "roundId": "provider-round-id",
                  "amount": 1.00,
                  "reference": "provider-bet-reference",
                  "providerId": "NOVA_REELS",
                  "timestamp": 1780000000000,
                  "token": "operator-generated-token"
                }
                """, ProviderBetRequest.class);

        assertEquals(null, request.roundDetails());
        assertEquals(null, request.platform());
        assertEquals(null, request.language());
        assertEquals(null, request.ipAddress());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void responseSerializesWithExactProviderFieldNames() throws Exception {
        ProviderBetResponse response = new ProviderBetResponse(
                TRANSACTION_ID,
                "EUR",
                new BigDecimal("999.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                0,
                "Success"
        );

        JsonNode actual = objectMapper.readTree(objectMapper.writeValueAsString(response));
        JsonNode expected = objectMapper.readTree("""
                {
                  "transactionId": "7035a82f-146a-42a0-ab95-07d867854ada",
                  "currency": "EUR",
                  "cash": 999.00,
                  "bonus": 0.00,
                  "usedPromo": 0.00,
                  "error": 0,
                  "description": "Success"
                }
                """);

        assertEquals(expected, actual);
    }

    @Test
    void requiredIdentifiersAndTokenMustNotBeBlank() {
        assertInvalidField(request(" ", "round", "reference", "provider", "token", amount("1")), "gameId");
        assertInvalidField(request("game", " ", "reference", "provider", "token", amount("1")), "roundId");
        assertInvalidField(request("game", "round", " ", "provider", "token", amount("1")), "reference");
        assertInvalidField(request("game", "round", "reference", " ", "token", amount("1")), "providerId");
        assertInvalidField(request("game", "round", "reference", "provider", " ", amount("1")), "token");
    }

    @Test
    void amountMustBePresentAndPositive() {
        assertInvalidField(request("game", "round", "reference", "provider", "token", null), "amount");
        assertInvalidField(request("game", "round", "reference", "provider", "token", BigDecimal.ZERO), "amount");
        assertInvalidField(request("game", "round", "reference", "provider", "token", amount("-1")), "amount");
    }

    @Test
    void userIdAndTimestampAreRequiredByProviderContract() {
        ProviderBetRequest missingUserId = new ProviderBetRequest(
                null,
                "game",
                "round",
                amount("1"),
                "reference",
                "provider",
                1780000000000L,
                null,
                null,
                null,
                "token",
                null
        );
        ProviderBetRequest missingTimestamp = new ProviderBetRequest(
                PLAYER_ID,
                "game",
                "round",
                amount("1"),
                "reference",
                "provider",
                null,
                null,
                null,
                null,
                "token",
                null
        );

        assertInvalidField(missingUserId, "userId");
        assertInvalidField(missingTimestamp, "timestamp");
    }

    private ProviderBetRequest request(
            String gameId,
            String roundId,
            String reference,
            String providerId,
            String token,
            BigDecimal amount
    ) {
        return new ProviderBetRequest(
                PLAYER_ID,
                gameId,
                roundId,
                amount,
                reference,
                providerId,
                1780000000000L,
                null,
                null,
                null,
                token,
                null
        );
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private void assertInvalidField(ProviderBetRequest request, String fieldName) {
        Set<ConstraintViolation<ProviderBetRequest>> violations = validator.validate(request);
        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(fieldName)));
    }
}
