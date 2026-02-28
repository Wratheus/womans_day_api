package com.womansday.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Login is required")
    @Size(min = 3, max = 50, message = "Login requires from 3 to 50 symbols")
    private String login;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password requires from 8 to 100 symbols")
    private String password;

    @NotBlank(message = "Name is required")
    private String firstName;

    @NotBlank(message = "LastName is required")
    private String lastName;

    @NotBlank(message = "Department is required")
    private String department;
}
