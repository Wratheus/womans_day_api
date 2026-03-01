package com.womansday.api.controller;

import com.womansday.api.dto.request.CreateTaskRequest;
import com.womansday.api.dto.request.ReviewRequest;
import com.womansday.api.dto.request.UpdateTaskRequest;
import com.womansday.api.dto.response.AdminSubmissionResponse;
import com.womansday.api.dto.response.TaskResponse;
import com.womansday.api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final TaskService taskService;

    @GetMapping("/submissions")
    public ResponseEntity<List<AdminSubmissionResponse>> getAllSubmissions(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(taskService.getAdminSubmissions(status));
    }

    @PatchMapping("/submissions/{submissionId}/review")
    public ResponseEntity<AdminSubmissionResponse> reviewSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(taskService.reviewSubmission(submissionId, request.getApproved()));
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
}
