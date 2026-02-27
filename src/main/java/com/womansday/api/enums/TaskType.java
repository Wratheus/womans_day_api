package com.womansday.api.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TaskType {
    @JsonProperty("photo")
    PHOTO,
    @JsonProperty("text")
    TEXT,
    @JsonProperty("textAndPhoto")
    TEXT_AND_PHOTO;
}
