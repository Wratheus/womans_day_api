package com.womansday.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class UserBalanceStatsResponse {
    private Long userId;
    private String login;
    private String firstName;
    private String lastName;
    private long currentBalance;
    private Map<String, Long> totalByType;
    private List<BalanceHistoryEntry> history;
}
