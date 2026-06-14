package com.boki0.casino.auth.service;

import com.boki0.casino.auth.dto.AuthUserResponse;
import com.boki0.casino.auth.dto.LoginRequest;
import com.boki0.casino.auth.dto.LoginResponse;
import com.boki0.casino.auth.dto.RegisterRequest;
import com.boki0.casino.auth.dto.RefreshTokenRequest;
import com.boki0.casino.auth.entity.AuthUser;
import com.boki0.casino.auth.entity.RefreshToken;
import com.boki0.casino.auth.enums.AccountStatus;
import com.boki0.casino.auth.enums.Role;
import com.boki0.casino.auth.repository.AuthUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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

    public LoginResponse login(LoginRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }

        String accessToken = jwtService.generateAccessToken(user);
        refreshTokenService.revokeAllUserTokens(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        AuthUserResponse userResponse = new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userResponse
        );
    }

    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken());
        AuthUser user = refreshToken.getUser();

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }

        String newRefreshToken = refreshTokenService.rotateRefreshToken(request.refreshToken());
        String accessToken = jwtService.generateAccessToken(user);
        AuthUserResponse userResponse = new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );

        return new LoginResponse(
                accessToken,
                newRefreshToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userResponse
        );
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());
    }

    public AuthUserResponse getCurrentUser(String email) {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }
}
