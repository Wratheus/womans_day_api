package com.womansday.api.service;

import com.womansday.api.dto.request.LoginRequest;
import com.womansday.api.dto.request.RegisterRequest;
import com.womansday.api.dto.response.AuthResponse;
import com.womansday.api.entity.User;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.enums.Role;
import com.womansday.api.repository.UserRepository;
import com.womansday.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final Set<String> revokedJtis = ConcurrentHashMap.newKeySet();

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

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new BusinessLogicException("Invalid login or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessLogicException("Invalid login or password");
        }

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
        if (jti != null && revokedJtis.contains(jti)) {
            throw new BusinessLogicException("Refresh token has been revoked");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("User not found"));

        if (jti != null) {
            revokedJtis.add(jti);
        }

        return buildAuthResponse(user);
    }

    public void logout(String refreshToken) {
        if (jwtTokenProvider.validateToken(refreshToken)) {
            String jti = jwtTokenProvider.getJti(refreshToken);
            if (jti != null) {
                revokedJtis.add(jti);
            }
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
