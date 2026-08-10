package com.boki0.casino.game.provider.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderResultDtoTest {

    private static final UUID USER_ID =
            UUID.fromString("c43e2215-8c9f-4a68-a179-94fcf4274ae9");
    private static final UUID TRANSACTION_ID =
            UUID.fromString("7035a82f-146a-42a0-ab95-07d867854ada");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validResultJsonDeserializesUsingExactProviderFields() throws Exception {
        ProviderResultRequest request = objectMapper.readValue("""
                {
                  "userId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                  "gameId": "NOVA_SEVEN",
                  "roundId": "provider-round-id",
                  "amount": 500.00,
                  "reference": "provider-result-reference",
                  "providerId": "NOVA_REELS",
                  "timestamp": 1780000001000,
                  "roundDetails": "spin",
                  "platform": "WEB",
                  "token": "operator-generated-token",
                  "bonusCode": "BONUS-1",
                  "promoWinAmount": 25.00,
                  "promoWinReference": "promo-win-1",
                  "promoCampaignID": "campaign-1",
                  "promoCampaignType": "FREE_SPINS"
                }
                """, ProviderResultRequest.class);

        assertEquals(USER_ID, request.userId());
        assertEquals("NOVA_SEVEN", request.gameId());
        assertEquals("provider-round-id", request.roundId());
        assertEquals(new BigDecimal("500.00"), request.amount());
        assertEquals("provider-result-reference", request.reference());
        assertEquals("NOVA_REELS", request.providerId());
        assertEquals(1780000001000L, request.timestamp());
        assertEquals("spin", request.roundDetails());
        assertEquals("WEB", request.platform());
        assertEquals("operator-generated-token", request.token());
        assertEquals("BONUS-1", request.bonusCode());
        assertEquals(new BigDecimal("25.00"), request.promoWinAmount());
        assertEquals("promo-win-1", request.promoWinReference());
        assertEquals("campaign-1", request.promoCampaignId());
        assertEquals("FREE_SPINS", request.promoCampaignType());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void zeroAndPositiveAmountsAreAcceptedButNegativeIsRejected() {
        assertTrue(validator.validate(request(BigDecimal.ZERO)).isEmpty());
        assertTrue(validator.validate(request(new BigDecimal("50.00"))).isEmpty());
        assertViolation(request(new BigDecimal("-0.01")), "amount");
    }

    @Test
    void requiredFieldsAreValidated() {
        assertViolation(copy(null, "GAME", "round", BigDecimal.ZERO, "ref", "PROVIDER", 1L, "token"), "userId");
        assertViolation(copy(USER_ID, " ", "round", BigDecimal.ZERO, "ref", "PROVIDER", 1L, "token"), "gameId");
        assertViolation(copy(USER_ID, "GAME", " ", BigDecimal.ZERO, "ref", "PROVIDER", 1L, "token"), "roundId");
        assertViolation(copy(USER_ID, "GAME", "round", null, "ref", "PROVIDER", 1L, "token"), "amount");
        assertViolation(copy(USER_ID, "GAME", "round", BigDecimal.ZERO, " ", "PROVIDER", 1L, "token"), "reference");
        assertViolation(copy(USER_ID, "GAME", "round", BigDecimal.ZERO, "ref", " ", 1L, "token"), "providerId");
        assertViolation(copy(USER_ID, "GAME", "round", BigDecimal.ZERO, "ref", "PROVIDER", null, "token"), "timestamp");
        assertViolation(copy(USER_ID, "GAME", "round", BigDecimal.ZERO, "ref", "PROVIDER", 1L, " "), "token");
    }

    @Test
    void optionalFieldsMayBeOmitted() throws Exception {
        ProviderResultRequest request = objectMapper.readValue("""
                {
                  "userId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                  "gameId": "NOVA_SEVEN",
                  "roundId": "provider-round-id",
                  "amount": 0,
                  "reference": "provider-result-reference",
                  "providerId": "NOVA_REELS",
                  "timestamp": 1780000001000,
                  "token": "operator-generated-token"
                }
                """, ProviderResultRequest.class);

        assertTrue(validator.validate(request).isEmpty());
        assertNull(request.roundDetails());
        assertNull(request.platform());
        assertNull(request.bonusCode());
        assertNull(request.promoWinAmount());
        assertNull(request.promoWinReference());
        assertNull(request.promoCampaignId());
        assertNull(request.promoCampaignType());
    }

    @Test
    void responseSerializesWithExactProviderFieldNamesAndNumericError() throws Exception {
        ProviderResultResponse response = new ProviderResultResponse(
                TRANSACTION_ID,
                "EUR",
                new BigDecimal("1480.00"),
                new BigDecimal("0.00"),
                0,
                "Success"
        );

        String json = objectMapper.writeValueAsString(response);

        assertEquals(TRANSACTION_ID.toString(), objectMapper.readTree(json).get("transactionId").asText());
        assertEquals("EUR", objectMapper.readTree(json).get("currency").asText());
        assertEquals(0, new BigDecimal("1480.00").compareTo(
                objectMapper.readTree(json).get("cash").decimalValue()
        ));
        assertEquals(0, new BigDecimal("0.00").compareTo(
                objectMapper.readTree(json).get("bonus").decimalValue()
        ));
        assertTrue(objectMapper.readTree(json).get("error").isInt());
        assertEquals(0, objectMapper.readTree(json).get("error").asInt());
        assertEquals("Success", objectMapper.readTree(json).get("description").asText());
        assertFalse(objectMapper.readTree(json).has("usedPromo"));
    }

    @Test
    void errorFactoryMatchesExistingProviderResponseConvention() {
        ProviderResultResponse response = ProviderResultResponse.error(1001, "Rejected");

        assertNull(response.transactionId());
        assertNull(response.currency());
        assertEquals(BigDecimal.ZERO, response.cash());
        assertEquals(BigDecimal.ZERO, response.bonus());
        assertEquals(1001, response.error());
        assertEquals("Rejected", response.description());
    }

    private ProviderResultRequest request(BigDecimal amount) {
        return copy(USER_ID, "NOVA_SEVEN", "round-1", amount, "result-1", "NOVA_REELS", 1L, "token");
    }

    private ProviderResultRequest copy(
            UUID userId,
            String gameId,
            String roundId,
            BigDecimal amount,
            String reference,
            String providerId,
            Long timestamp,
            String token
    ) {
        return new ProviderResultRequest(
                userId, gameId, roundId, amount, reference, providerId, timestamp,
                null, null, token, null, null, null, null, null
        );
    }

    private void assertViolation(ProviderResultRequest request, String property) {
        Set<ConstraintViolation<ProviderResultRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(violation -> property.equals(
                violation.getPropertyPath().toString()
        )));
    }
}
