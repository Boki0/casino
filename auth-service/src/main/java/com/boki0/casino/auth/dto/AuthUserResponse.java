package com.boki0.casino.auth.dto;

import com.boki0.casino.auth.enums.AccountStatus;
import com.boki0.casino.auth.enums.Role;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        Role role,
        AccountStatus status
) {
}
