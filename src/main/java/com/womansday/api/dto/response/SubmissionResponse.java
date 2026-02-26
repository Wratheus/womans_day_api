package com.womansday.api.dto.response;

import com.womansday.api.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Long userId;
    private String userLogin;
    private String userDepartment;
    private SubmissionStatus status;
    private String text;
    private LocalDateTime createdAt;
    private List<Long> photoIds;
}
