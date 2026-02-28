package com.womansday.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SubmissionStatus {
    NOT_STARTED("notStarted"),
    WAITING_FOR_PARTICIPANTS("waitingForParticipants"),
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
