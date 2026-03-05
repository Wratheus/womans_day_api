package com.womansday.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Статус одобрения обязателен")
    private Boolean approved;

    private String comment;
}
