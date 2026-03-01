package com.womansday.api.dto.request;

import com.womansday.api.enums.TaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTaskRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Min(value = 1, message = "Reward must be at least 1")
    private Integer reward;

    private TaskType type;

    private Boolean collaborative;
}
