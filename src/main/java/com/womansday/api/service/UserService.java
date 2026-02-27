package com.womansday.api.service;

import com.womansday.api.dto.request.UpdateProfileRequest;
import com.womansday.api.dto.response.MeResponse;
import com.womansday.api.dto.response.UserResponse;
import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.exception.ResourceNotFoundException;
import com.womansday.api.repository.TaskSubmissionRepository;
import com.womansday.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final PhotoStorageService photoStorageService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(Role callerRole) {
        List<User> users;
        if (callerRole == Role.ADMIN) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findByRoleNot(Role.ADMIN);
        }

        return users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
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
                .avatarUrl(buildAvatarUrl(user))
                .build();
    }

    @Transactional
    @SuppressWarnings("null")
    public MeResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }

        userRepository.save(user);

        long balance = submissionRepository.sumApprovedRewardsByUserId(userId);

        return MeResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .balance(balance)
                .avatarUrl(buildAvatarUrl(user))
                .build();
    }

    @Transactional
    @SuppressWarnings("null")
    public String uploadAvatar(Long userId, MultipartFile avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        String contentType = avatar.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessLogicException("Допускаются только изображения");
        }

        try {
            if (user.getAvatarPath() != null) {
                photoStorageService.delete(user.getAvatarPath());
            }

            String path = photoStorageService.storeAvatar(userId, contentType, avatar.getBytes());
            user.setAvatarPath(path);
            user.setAvatarContentType(contentType);
            userRepository.save(user);

            return "/api/users/" + userId + "/avatar";
        } catch (IOException e) {
            throw new BusinessLogicException("Ошибка загрузки аватара");
        }
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .department(user.getDepartment())
                .avatarUrl(buildAvatarUrl(user))
                .build();
    }

    private String buildAvatarUrl(User user) {
        return user.getAvatarPath() != null ? "/api/users/" + user.getId() + "/avatar" : null;
    }
}
