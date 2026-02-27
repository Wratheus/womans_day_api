package com.womansday.api.service;

import com.womansday.api.dto.response.MeResponse;
import com.womansday.api.dto.response.UserResponse;
import com.womansday.api.entity.User;
import com.womansday.api.exception.ResourceNotFoundException;
import com.womansday.api.repository.TaskSubmissionRepository;
import com.womansday.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TaskSubmissionRepository submissionRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .login(user.getLogin())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .department(user.getDepartment())
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("null")
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        long balance = submissionRepository.sumApprovedRewardsByUserId(userId);

        return MeResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .balance(balance)
                .build();
    }
}
