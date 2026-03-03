package com.womansday.api.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TaskType {
    @JsonProperty("text")
    TEXT,
    @JsonProperty("media")
    MEDIA,
    @JsonProperty("textAndMedia")
    TEXT_AND_MEDIA;
}
