package com.womansday.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Логин обязателен")
    @Size(min = 3, max = 50, message = "Логин от 3 до 50 символов")
    private String login;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 4, max = 100, message = "Пароль от 4 до 100 символов")
    private String password;

    @NotBlank(message = "Отдел обязателен")
    private String department;
}
