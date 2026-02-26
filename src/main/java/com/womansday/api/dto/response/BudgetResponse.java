package com.womansday.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BudgetResponse {
    private long totalBudget;
    private long approvedBudget;
}
