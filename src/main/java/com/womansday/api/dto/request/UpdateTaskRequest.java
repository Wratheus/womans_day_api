package com.womansday.api.dto.request;

import com.womansday.api.enums.TaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTaskRequest {

    @Size(max = 200, message = "Название не должно превышать 200 символов")
    private String title;

    @Size(max = 5000, message = "Описание не должно превышать 5000 символов")
    private String description;

    @Min(value = 1, message = "Вознаграждение должно быть не менее 1")
    private Integer reward;

    private TaskType type;

    private Boolean collaborative;
}
