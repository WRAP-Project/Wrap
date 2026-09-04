package com.wrap.domain.schedule.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ScheduleType {
    DEADLINE("deadline"),
    MEETING("meeting"),
    MILESTONE("milestone");

    private final String value;

    ScheduleType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ScheduleType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ScheduleType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported schedule type: " + value);
    }
}
