package com.womansday.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BonusPointsRequest {

    @NotNull(message = "Сумма бонуса обязательна")
    private Integer amount;
}
