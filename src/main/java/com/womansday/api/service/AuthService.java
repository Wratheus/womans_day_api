package com.womansday.api.service;

import com.womansday.api.dto.request.LoginRequest;
import com.womansday.api.dto.request.RegisterRequest;
import com.womansday.api.dto.response.AuthResponse;
import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import com.womansday.api.repository.UserRepository;
import com.womansday.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new IllegalArgumentException("Логин уже занят");
        }

        User user = User.builder()
                .login(request.getLogin())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .department(request.getDepartment())
                .role(Role.USER)
                .build();

        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getLogin(), user.getRole().name());

        return AuthResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .department(user.getDepartment())
                .role(user.getRole())
                .token(token)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new IllegalArgumentException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный логин или пароль");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getLogin(), user.getRole().name());

        return AuthResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .department(user.getDepartment())
                .role(user.getRole())
                .token(token)
                .build();
    }
}
