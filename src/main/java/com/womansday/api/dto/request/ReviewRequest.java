package com.womansday.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "userId обязателен")
    private Long userId;

    @NotNull(message = "approved обязателен")
    private Boolean approved;
}
