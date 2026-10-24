package kuchma.mike.bbdm.domain.model;

import java.util.Objects;

public record ShopItemId(String value) {
    public ShopItemId {
        Objects.requireNonNull(value, "Shop item ID is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Shop item ID must not be blank");
        }
    }
}
