package com.boki0.casino.game.api.dto;

import jakarta.validation.constraints.NotBlank;

public record GameLaunchRequest(
        @NotBlank(message = "currency must not be blank") String currency
) {
}
