package com.womansday.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class LootBoxStatsResponse {
    private long totalOpened;
    private long totalUnopened;
    private long totalPrizeSum;
    private List<TierStats> tiers;

    @Data
    @Builder
    @AllArgsConstructor
    public static class TierStats {
        private int prizeAmount;
        private int configuredWeight;
        private double configuredChance;
        private long actualCount;
        private double actualChance;
    }
}
