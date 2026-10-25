package kuchma.mike.bbdm.domain.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ItemId(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ItemId {
        Objects.requireNonNull(value, "Item ID is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Item ID must not be blank");
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
