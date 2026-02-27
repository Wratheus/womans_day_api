package com.womansday.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SubmissionStatus {
    NOT_STARTED("notStarted"),
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String value;

    SubmissionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
