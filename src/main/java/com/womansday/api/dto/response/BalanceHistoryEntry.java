package com.womansday.api.dto.response;

import com.womansday.api.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BalanceHistoryEntry {
    private Long id;
    private TransactionType type;
    private Integer amount;
    private String description;
    private Long createdAtEpoch;
}
