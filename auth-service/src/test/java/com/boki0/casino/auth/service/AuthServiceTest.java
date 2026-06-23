package com.boki0.casino.auth.service;

import com.boki0.casino.auth.dto.AuthUserResponse;
import com.boki0.casino.auth.dto.LoginRequest;
import com.boki0.casino.auth.dto.LoginResponse;
import com.boki0.casino.auth.dto.RefreshTokenRequest;
import com.boki0.casino.auth.dto.RegisterRequest;
import com.boki0.casino.auth.entity.AuthUser;
import com.boki0.casino.auth.enums.AccountStatus;
import com.boki0.casino.auth.enums.Role;
import com.boki0.casino.auth.event.DomainEvent;
import com.boki0.casino.auth.event.DomainEventPublisher;
import com.boki0.casino.auth.event.UserRegisteredEvent;
import com.boki0.casino.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUser_whenEmailIsNew() {
        String email = "player@example.com";
        String password = "password123";
        String username = "player123";
        String passwordHash = "encoded-password";
        RegisterRequest request = new RegisterRequest(email, password, username, null);
        UUID userId = UUID.randomUUID();

        when(authUserRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(passwordHash);
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> {
            AuthUser user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        AuthUserResponse response = authService.register(request);

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(passwordEncoder).encode(password);
        verify(authUserRepository).save(userCaptor.capture());
        verify(domainEventPublisher).publish(eventCaptor.capture());

        AuthUser savedUser = userCaptor.getValue();
        UserRegisteredEvent event = assertInstanceOf(UserRegisteredEvent.class, eventCaptor.getValue());
        assertEquals(email, savedUser.getEmail());
        assertEquals(passwordHash, savedUser.getPasswordHash());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals(AccountStatus.ACTIVE, savedUser.getStatus());
        assertNotNull(event.eventId());
        assertEquals("USER_REGISTERED", event.eventType());
        assertEquals(1, event.eventVersion());
        assertEquals(userId, event.authUserId());
        assertEquals(email, event.email());
        assertEquals(username, event.username());
        assertNotNull(event.occurredAt());
        assertEquals(email, response.email());
        assertEquals(Role.USER, response.role());
        assertEquals(AccountStatus.ACTIVE, response.status());
        assertFalse(Arrays.stream(AuthUserResponse.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("password")
                        || component.getName().equals("passwordHash")));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        String email = "player@example.com";
        RegisterRequest request = new RegisterRequest(email, "password123", "player123", null);

        when(authUserRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(authUserRepository, never()).save(any(AuthUser.class));
        verify(domainEventPublisher, never()).publish(any(DomainEvent.class));
    }

    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid() {
        String email = "player@example.com";
        String password = "password123";
        String passwordHash = "encoded-password";
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        long expiresIn = 900L;
        LoginRequest request = new LoginRequest(email, password);
        AuthUser user = new AuthUser(email, passwordHash, Role.USER, AccountStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        user.setId(userId);

        when(authUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, passwordHash)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtService.getExpirationSeconds()).thenReturn(expiresIn);
        doNothing().when(refreshTokenService).revokeAllUserTokens(user);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        LoginResponse response = authService.login(request);

        verify(refreshTokenService).revokeAllUserTokens(user);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(expiresIn, response.expiresIn());
        assertEquals(userId, response.user().id());
        assertEquals(email, response.user().email());
        assertEquals(Role.USER, response.user().role());
        assertEquals(AccountStatus.ACTIVE, response.user().status());
    }

    @Test
    void login_shouldThrowException_whenUserDoesNotExist() {
        String email = "player@example.com";
        LoginRequest request = new LoginRequest(email, "password123");

        when(authUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowException_whenPasswordIsInvalid() {
        String email = "player@example.com";
        String password = "password123";
        String passwordHash = "encoded-password";
        LoginRequest request = new LoginRequest(email, password);
        AuthUser user = new AuthUser(email, passwordHash, Role.USER, AccountStatus.ACTIVE);

        when(authUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, passwordHash)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void logout_shouldRevokeRefreshToken() {
        String refreshToken = "refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        authService.logout(request);

        verify(refreshTokenService).revokeRefreshToken(refreshToken);
    }
}
