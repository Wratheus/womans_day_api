package com.womansday.api.controller;

import com.womansday.api.dto.request.ReviewRequest;
import com.womansday.api.dto.response.BudgetResponse;
import com.womansday.api.dto.response.SubmissionResponse;
import com.womansday.api.dto.response.TaskResponse;
import com.womansday.api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submitTask(
            @PathVariable Long id,
            @RequestParam(required = false) String text,
            @RequestPart(required = false) List<MultipartFile> photos,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.submitTask(id, userId, text, photos));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<SubmissionResponse> reviewSubmission(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(taskService.reviewSubmission(id, request.getUserId(), request.getApproved()));
    }

    @GetMapping("/budget")
    public ResponseEntity<BudgetResponse> getBudget() {
        return ResponseEntity.ok(taskService.getBudget());
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long photoId) {
        byte[] data = taskService.getPhotoData(photoId);
        String contentType = taskService.getPhotoContentType(photoId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }
}
