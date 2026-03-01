package com.womansday.api.service;

import com.womansday.api.dto.request.LoginRequest;
import com.womansday.api.dto.request.RegisterRequest;
import com.womansday.api.dto.response.AuthResponse;
import com.womansday.api.entity.RevokedToken;
import com.womansday.api.entity.User;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.enums.Role;
import com.womansday.api.repository.RevokedTokenRepository;
import com.womansday.api.repository.UserRepository;
import com.womansday.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @SuppressWarnings("null")
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new BusinessLogicException("Login is already taken");
        }

        User user = User.builder()
                .login(request.getLogin())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .department(request.getDepartment())
                .role(Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("User registered: login={}", user.getLogin());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> {
                    log.warn("Login attempt with unknown login: {}", request.getLogin());
                    return new BusinessLogicException("Invalid login or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for user: {}", request.getLogin());
            throw new BusinessLogicException("Invalid login or password");
        }

        log.info("User logged in: login={}", user.getLogin());
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessLogicException("Invalid refresh token");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessLogicException("Invalid token type");
        }

        String jti = jwtTokenProvider.getJti(refreshToken);
        if (jti != null && revokedTokenRepository.existsByJti(jti)) {
            throw new BusinessLogicException("Refresh token has been revoked");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("User not found"));

        if (jti != null) {
            revokeToken(jti, refreshToken);
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessLogicException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for userId={}", userId);
    }

    public void logout(String refreshToken) {
        if (jwtTokenProvider.validateToken(refreshToken)) {
            String jti = jwtTokenProvider.getJti(refreshToken);
            if (jti != null) {
                revokeToken(jti, refreshToken);
            }
        }
    }

    @SuppressWarnings("null")
    private void revokeToken(String jti, String token) {
        Instant expiresAt = jwtTokenProvider.getExpiration(token).toInstant();
        RevokedToken revoked = RevokedToken.builder()
                .jti(jti)
                .expiresAt(expiresAt)
                .build();
        revokedTokenRepository.save(revoked);
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = revokedTokenRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired revoked tokens", deleted);
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getLogin(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .department(user.getDepartment())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessExpirationSeconds())
                .build();
    }
}
