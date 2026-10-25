package kuchma.mike.bbdm.domain.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ShopItemId(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ShopItemId {
        Objects.requireNonNull(value, "Shop item ID is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Shop item ID must not be blank");
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
