package com.womansday.api.controller;

import com.womansday.api.dto.request.BonusPointsRequest;
import com.womansday.api.dto.request.CreateTaskRequest;
import com.womansday.api.dto.request.ResetPasswordRequest;
import com.womansday.api.dto.request.ReviewRequest;
import com.womansday.api.dto.request.UpdateTaskRequest;
import com.womansday.api.dto.response.AdminSubmissionResponse;
import com.womansday.api.dto.response.LeaderboardEntry;
import com.womansday.api.dto.response.TaskResponse;
import com.womansday.api.dto.response.TaskStatsResponse;
import com.womansday.api.dto.response.UserResponse;
import com.womansday.api.enums.Role;
import com.womansday.api.service.AuthService;
import com.womansday.api.service.LootBoxService;
import com.womansday.api.service.MediaStorageService;
import com.womansday.api.service.TaskService;
import com.womansday.api.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final LootBoxService lootBoxService;
    private final MediaStorageService mediaStorageService;
    private final TaskService taskService;
    private final UserService userService;

    @GetMapping("/submissions")
    public ResponseEntity<List<AdminSubmissionResponse>> getAllSubmissions(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(taskService.getAdminSubmissions(status));
    }

    @PatchMapping("/submissions/{submissionId}/cancel")
    public ResponseEntity<Void> cancelSubmission(@PathVariable Long submissionId) {
        taskService.adminCancelSubmission(submissionId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/submissions/{submissionId}/review")
    public ResponseEntity<AdminSubmissionResponse> reviewSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(taskService.reviewSubmission(submissionId, request.getApproved(), request.getComment()));
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/hide")
    public ResponseEntity<UserResponse> hideUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.hideUser(id));
    }

    @PatchMapping("/users/{id}/reveal")
    public ResponseEntity<UserResponse> revealUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.revealUser(id));
    }

    @PatchMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(id, request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{id}/bonus")
    public ResponseEntity<UserResponse> setBonusPoints(
            @PathVariable Long id,
            @Valid @RequestBody BonusPointsRequest request) {
        return ResponseEntity.ok(userService.setBonusPoints(id, request.getBonusPoints()));
    }

    @GetMapping("/tasks/{id}/stats")
    public ResponseEntity<TaskStatsResponse> getTaskStats(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskStats(id));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        return ResponseEntity.ok(taskService.getLeaderboard(Role.ADMIN));
    }

    @PostMapping("/lootbox/gift-all")
    public ResponseEntity<Map<String, Integer>> giftLootBoxToAll() {
        int count = lootBoxService.giftToAll();
        return ResponseEntity.ok(Map.of("gifted", count));
    }

    @GetMapping("/media/export")
    public void exportMedia(HttpServletResponse response) throws Exception {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"media.zip\"");
        mediaStorageService.streamAllAsZip(response.getOutputStream());
    }
}
