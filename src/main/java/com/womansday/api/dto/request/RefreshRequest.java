package com.womansday.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "RefreshToken is required")
    private String refreshToken;
}
