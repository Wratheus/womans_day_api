package com.womansday.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 50, message = "Имя от 1 до 50 символов")
    private String firstName;

    @Size(min = 1, max = 50, message = "Фамилия от 1 до 50 символов")
    private String lastName;

    @Size(min = 1, max = 100, message = "Отдел от 1 до 100 символов")
    private String department;
}
