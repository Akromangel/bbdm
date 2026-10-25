package kuchma.mike.bbdm.domain.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record MessageId(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public MessageId {
        Objects.requireNonNull(value, "Message ID is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Message ID must not be blank");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
