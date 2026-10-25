package kuchma.mike.bbdm.domain.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record GameId(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public GameId {
        Objects.requireNonNull(value, "Game ID is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Game ID must not be blank");
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
