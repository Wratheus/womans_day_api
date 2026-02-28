package com.womansday.api.controller;

import com.womansday.api.dto.response.BudgetResponse;
import com.womansday.api.dto.response.SubmissionResponse;
import com.womansday.api.dto.response.TaskResponse;
import com.womansday.api.entity.SubmissionPhoto;
import com.womansday.api.service.PhotoStorageService;
import com.womansday.api.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final PhotoStorageService photoStorageService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.getAllTasks(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.getTask(id, userId));
    }

    @PostMapping(value = "/{id}/submit", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submitTask(
            @PathVariable Long id,
            @RequestParam(required = false) String text,
            @RequestPart(required = false) List<MultipartFile> photos,
            @RequestParam(required = false) List<Long> participantIds,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.submitTask(id, userId, text, photos, participantIds));
    }

    @PostMapping("/submissions/{submissionId}/respond")
    public ResponseEntity<Void> respondToInvitation(
            @PathVariable Long submissionId,
            @RequestParam boolean accept,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        if (accept) {
            taskService.acceptInvitation(submissionId, userId);
        } else {
            taskService.declineInvitation(submissionId, userId);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/budget")
    public ResponseEntity<BudgetResponse> getBudget() {
        return ResponseEntity.ok(taskService.getBudget());
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long photoId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = extractRoleString(authentication);

        SubmissionPhoto photo = taskService.getPhoto(photoId, userId, role);

        try {
            byte[] data = photoStorageService.load(photo.getFilePath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, photo.getContentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String extractRoleString(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("USER");
    }
}
