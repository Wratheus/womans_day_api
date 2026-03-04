package com.womansday.api.controller;

import com.womansday.api.dto.response.BudgetResponse;
import com.womansday.api.dto.response.SubmissionResponse;
import com.womansday.api.dto.response.TaskResponse;
import com.womansday.api.entity.SubmissionPhoto;
import com.womansday.api.security.SecurityUtils;
import com.womansday.api.service.PhotoStorageService;
import com.womansday.api.service.TaskService;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final PhotoStorageService photoStorageService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        return ResponseEntity.ok(taskService.getAllTasks(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id, Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        return ResponseEntity.ok(taskService.getTask(id, userId));
    }

    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submitTask(
            @PathVariable Long id,
            @RequestParam(required = false) String text,
            @RequestPart(required = false) List<MultipartFile> files,
            @RequestParam(required = false) List<Long> participantIds,
            Authentication authentication) {

        Long userId = SecurityUtils.extractUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.submitTask(id, userId, text, files, participantIds));
    }

    @PostMapping("/submissions/{submissionId}/respond")
    public ResponseEntity<Void> respondToInvitation(
            @PathVariable Long submissionId,
            @RequestParam boolean accept,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        if (accept) {
            taskService.acceptInvitation(submissionId, userId);
        } else {
            taskService.declineInvitation(submissionId, userId);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/submissions/{submissionId}")
    public ResponseEntity<Void> cancelSubmission(
            @PathVariable Long submissionId,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        taskService.cancelSubmission(submissionId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/budget")
    public ResponseEntity<BudgetResponse> getBudget() {
        return ResponseEntity.ok(taskService.getBudget());
    }

    @SuppressWarnings("null")
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> getFile(@PathVariable Long fileId, Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        String role = SecurityUtils.extractRoleString(authentication);

        SubmissionPhoto file = taskService.getPhoto(fileId, userId, role);

        try {
            Path path = photoStorageService.resolvePathByKey(file.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
