package com.womansday.api.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Role {
    @JsonProperty("user")
    USER,
    @JsonProperty("admin")
    ADMIN;
}
