package com.womansday.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LootBoxResponse {
    private Long id;
    private Integer cost;
    private Integer prizeAmount;
    private Long openedAtEpoch;
    private Long createdAtEpoch;
}
