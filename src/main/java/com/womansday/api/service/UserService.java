package com.womansday.api.service;

import com.womansday.api.dto.request.UpdateProfileRequest;
import com.womansday.api.dto.response.MeResponse;
import com.womansday.api.dto.response.UserResponse;
import com.womansday.api.entity.BalanceTransaction;
import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import com.womansday.api.enums.TransactionType;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.exception.ResourceNotFoundException;
import com.womansday.api.repository.BalanceTransactionRepository;
import com.womansday.api.repository.LootBoxRepository;
import com.womansday.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BalanceTransactionRepository balanceTransactionRepository;
    private final LootBoxRepository lootBoxRepository;
    private final MediaStorageService mediaStorageService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(Role callerRole) {
        List<User> users;
        if (callerRole == Role.ADMIN) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findVisibleByRoleNot(Role.ADMIN);
        }

        return users.stream()
                .map(u -> toUserResponse(u, callerRole))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse hideUser(Long userId) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        user.setHidden(true);
        userRepository.save(user);
        log.info("User hidden: userId={}", userId);
        return toUserResponse(user, Role.ADMIN);
    }

    @Transactional
    public UserResponse revealUser(Long userId) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        user.setHidden(false);
        userRepository.save(user);
        log.info("User revealed: userId={}", userId);
        return toUserResponse(user, Role.ADMIN);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return toMeResponse(user);
    }

    @Transactional
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
        return toMeResponse(user);
    }

    @Transactional
    public String uploadAvatar(Long userId, MultipartFile avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        String contentType = avatar.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessLogicException("Разрешены только изображения");
        }

        try {
            if (user.getAvatarPath() != null) {
                mediaStorageService.deleteByKey(user.getAvatarPath());
            }

            try (InputStream in = avatar.getInputStream()) {
                String path = mediaStorageService.storeAvatar(userId, contentType, in);
                user.setAvatarPath(path);
                user.setAvatarContentType(contentType);
                userRepository.save(user);
            }

            return "/api/users/" + userId + "/avatar";
        } catch (IOException e) {
            throw new BusinessLogicException("Ошибка загрузки аватара");
        }
    }

    @Transactional
    public void deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (user.getAvatarPath() == null) {
            throw new BusinessLogicException("Аватар отсутствует");
        }

        try {
            mediaStorageService.deleteByKey(user.getAvatarPath());
        } catch (IOException ignored) {
        }

        user.setAvatarPath(null);
        user.setAvatarContentType(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    @Transactional
    public UserResponse setBonusPoints(Long userId, int bonusPoints) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (bonusPoints != 0) {
            balanceTransactionRepository.save(BalanceTransaction.builder()
                    .user(user)
                    .type(TransactionType.BONUS)
                    .amount(bonusPoints)
                    .description("Бонус от администратора")
                    .build());
        }

        log.info("Bonus points added: userId={}, amount={}", userId, bonusPoints);
        return toUserResponse(user, Role.ADMIN);
    }

    long calculateBalance(Long userId) {
        return balanceTransactionRepository.sumByUserId(userId);
    }

    private MeResponse toMeResponse(User user) {
        return MeResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .balance(calculateBalance(user.getId()))
                .hasAvatar(user.getAvatarPath() != null)
                .unopenedBoxes(lootBoxRepository.countUnopenedByUserId(user.getId()))
                .build();
    }

    private UserResponse toUserResponse(User user, Role callerRole) {
        return UserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .department(user.getDepartment())
                .hasAvatar(user.getAvatarPath() != null)
                .hidden(callerRole == Role.ADMIN ? Boolean.TRUE.equals(user.getHidden()) : null)
                .build();
    }

}
