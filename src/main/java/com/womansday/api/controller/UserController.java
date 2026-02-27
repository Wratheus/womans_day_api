package com.womansday.api.controller;

import com.womansday.api.dto.request.UpdateProfileRequest;
import com.womansday.api.dto.response.MeResponse;
import com.womansday.api.dto.response.UserResponse;
import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import com.womansday.api.service.PhotoStorageService;
import com.womansday.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PhotoStorageService photoStorageService;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<MeResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestPart MultipartFile avatar,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String url = userService.uploadAvatar(userId, avatar);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        User user = userService.getUserEntity(id);
        if (user.getAvatarPath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] data = photoStorageService.load(user.getAvatarPath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, user.getAvatarContentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        Role callerRole = extractRole(authentication);
        return ResponseEntity.ok(userService.getAllUsers(callerRole));
    }

    private Role extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .map(r -> {
                    try { return Role.valueOf(r); }
                    catch (IllegalArgumentException e) { return Role.USER; }
                })
                .orElse(Role.USER);
    }
}
