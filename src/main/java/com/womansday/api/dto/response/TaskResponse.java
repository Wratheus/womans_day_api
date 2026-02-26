package com.womansday.api.dto.response;

import com.womansday.api.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Integer reward;
    private TaskType type;
    private List<SubmissionResponse> submissions;
}
