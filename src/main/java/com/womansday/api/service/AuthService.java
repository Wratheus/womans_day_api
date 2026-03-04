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

import org.springframework.dao.DataIntegrityViolationException;
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

    private static final int MAX_USERS = 140;

    @SuppressWarnings("null")
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.countByRoleNot(Role.ADMIN) >= MAX_USERS) {
            throw new BusinessLogicException("Регистрация закрыта. Достигнут лимит пользователей");
        }

        String login = request.getLogin().trim().toLowerCase();

        if (userRepository.existsByLogin(login)) {
            throw new BusinessLogicException("Этот логин уже занят");
        }

        User user = User.builder()
                .login(login)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .department(request.getDepartment())
                .role(Role.USER)
                .build();

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessLogicException("Этот логин уже занят");
        }

        log.info("User registered: login={}", user.getLogin());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> {
                    log.warn("Login attempt with unknown login: {}", request.getLogin());
                    return new BusinessLogicException("Неверный логин или пароль");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for user: {}", request.getLogin());
            throw new BusinessLogicException("Неверный логин или пароль");
        }

        log.info("User logged in: login={}", user.getLogin());
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessLogicException("Недействительный токен обновления");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessLogicException("Неверный тип токена");
        }

        String jti = jwtTokenProvider.getJti(refreshToken);
        if (jti != null && revokedTokenRepository.existsByJti(jti)) {
            throw new BusinessLogicException("Токен обновления был отозван");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("Пользователь не найден"));

        if (jti != null) {
            revokeToken(jti, refreshToken);
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("Пользователь не найден"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessLogicException("Текущий пароль неверен");
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
