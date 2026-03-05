package com.womansday.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BonusPointsRequest {

    @NotNull(message = "Количество бонусных баллов обязательно")
    private Integer bonusPoints;
}
