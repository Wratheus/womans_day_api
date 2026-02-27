package com.womansday.api.enums;

public enum SubmissionStatus {
    PENDING,
    APPROVED,
    REJECTED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
