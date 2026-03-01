package com.womansday.api.dto.request;

import com.womansday.api.enums.TaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Reward is required")
    @Min(value = 1, message = "Reward must be at least 1")
    private Integer reward;

    @NotNull(message = "Type is required")
    private TaskType type;

    private Boolean collaborative = false;
}
