package com.womansday.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Firstname requires from 2 to 50 symbols")
    private String firstName;

    @Size(min = 2, max = 50, message = "Lastname requires from 2 to 50 symbols")
    private String lastName;

    @Size(min = 2, max = 100, message = "Department requires from 1 to 100 symbols")
    private String department;
}
