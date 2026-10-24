package kuchma.mike.bbdm.domain.model;

import java.util.Objects;

public record GameId(String value) {
    public GameId {
        Objects.requireNonNull(value, "Game ID is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Game ID must not be blank");
        }
    }
}
