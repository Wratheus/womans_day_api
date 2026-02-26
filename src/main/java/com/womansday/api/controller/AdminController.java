package com.womansday.api.controller;

import com.womansday.api.dto.request.ReviewRequest;
import com.womansday.api.dto.response.AdminSubmissionResponse;
import com.womansday.api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
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
}
