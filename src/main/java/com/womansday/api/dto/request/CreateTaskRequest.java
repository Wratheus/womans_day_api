package com.womansday.api.dto.request;

import com.womansday.api.enums.TaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Название обязательно")
    @Size(max = 200, message = "Название не должно превышать 200 символов")
    private String title;

    @Size(max = 5000, message = "Описание не должно превышать 5000 символов")
    private String description;

    @NotNull(message = "Вознаграждение обязательно")
    @Min(value = 1, message = "Вознаграждение должно быть не менее 1")
    private Integer reward;

    @NotNull(message = "Тип обязателен")
    private TaskType type;

    private Boolean collaborative = false;
}
