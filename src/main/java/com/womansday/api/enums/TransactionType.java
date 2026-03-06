package com.womansday.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    TASK_REWARD("taskReward"),
    BONUS("bonus"),
    LOOTBOX_PRIZE("lootboxPrize");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
