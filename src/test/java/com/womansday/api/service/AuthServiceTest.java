package com.womansday.api.service;

import com.womansday.api.dto.request.LoginRequest;
import com.womansday.api.dto.request.RegisterRequest;
import com.womansday.api.dto.response.AuthResponse;
import com.womansday.api.entity.RevokedToken;
import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.repository.RevokedTokenRepository;
import com.womansday.api.repository.UserRepository;
import com.womansday.api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .login("testuser")
                .passwordHash("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .department("IT")
                .role(Role.USER)
                .build();
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("newuser");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");
        request.setDepartment("IT");

        when(userRepository.existsByLogin("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyLong()))
                .thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowWhenLoginTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("testuser");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setDepartment("IT");

        when(userRepository.existsByLogin("testuser")).thenReturn(true);

        assertThrows(BusinessLogicException.class, () -> authService.register(request));
    }

    @Test
    void login_shouldReturnTokensForValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setLogin("testuser");
        request.setPassword("password123");

        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(1L, "testuser", "USER"))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(1L))
                .thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("testuser", response.getLogin());
    }

    @Test
    void login_shouldThrowForInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setLogin("testuser");
        request.setPassword("wrongpassword");

        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThrows(BusinessLogicException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowForNonexistentUser() {
        LoginRequest request = new LoginRequest();
        request.setLogin("unknown");
        request.setPassword("password");

        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessLogicException.class, () -> authService.login(request));
    }

    @Test
    void refresh_shouldRevokeOldAndReturnNewTokens() {
        String refreshToken = "valid-refresh-token";

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getJti(refreshToken)).thenReturn("jti-123");
        when(revokedTokenRepository.existsByJti("jti-123")).thenReturn(false);
        when(jwtTokenProvider.getUserId(refreshToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.getExpiration(refreshToken))
                .thenReturn(new java.util.Date(System.currentTimeMillis() + 3600000));
        when(jwtTokenProvider.generateAccessToken(1L, "testuser", "USER"))
                .thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(1L))
                .thenReturn("new-refresh");
        when(jwtTokenProvider.getAccessExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.refresh(refreshToken);

        assertNotNull(response);
        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        assertEquals("jti-123", captor.getValue().getJti());
    }

    @Test
    void refresh_shouldThrowForRevokedToken() {
        String refreshToken = "revoked-token";

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getJti(refreshToken)).thenReturn("jti-revoked");
        when(revokedTokenRepository.existsByJti("jti-revoked")).thenReturn(true);

        assertThrows(BusinessLogicException.class, () -> authService.refresh(refreshToken));
    }

    @Test
    void logout_shouldRevokeToken() {
        String refreshToken = "valid-refresh-token";

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getJti(refreshToken)).thenReturn("jti-456");
        when(jwtTokenProvider.getExpiration(refreshToken))
                .thenReturn(new java.util.Date(System.currentTimeMillis() + 3600000));

        authService.logout(refreshToken);

        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void changePassword_shouldUpdatePasswordHash() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPass", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newHashedPassword");

        authService.changePassword(1L, "currentPass", "newPass123");

        assertEquals("newHashedPassword", testUser.getPasswordHash());
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_shouldThrowForWrongCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPass", "hashedPassword")).thenReturn(false);

        assertThrows(BusinessLogicException.class,
                () -> authService.changePassword(1L, "wrongPass", "newPass123"));
    }
}
