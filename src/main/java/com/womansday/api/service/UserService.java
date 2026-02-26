package com.womansday.api.service;

import com.womansday.api.dto.response.UserResponse;
import com.womansday.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .login(user.getLogin())
                        .department(user.getDepartment())
                        .build())
                .collect(Collectors.toList());
    }
}
