package com.womansday.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Approved status is required")
    private Boolean approved;
}
