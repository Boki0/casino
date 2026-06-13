package com.boki0.casino.auth.service;

import com.boki0.casino.auth.dto.AuthUserResponse;
import com.boki0.casino.auth.dto.RegisterRequest;
import com.boki0.casino.auth.entity.AuthUser;
import com.boki0.casino.auth.enums.AccountStatus;
import com.boki0.casino.auth.enums.Role;
import com.boki0.casino.auth.repository.AuthUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthUserResponse register(RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        AuthUser authUser = new AuthUser(
                request.email(),
                passwordHash,
                Role.USER,
                AccountStatus.ACTIVE
        );

        AuthUser savedUser = authUserRepository.save(authUser);

        return new AuthUserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getStatus()
        );
    }
}
