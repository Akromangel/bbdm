package kuchma.mike.bbdm.domain.model;

import java.util.Objects;

public record MessageId(String value) {
    public MessageId {
        Objects.requireNonNull(value, "Message ID is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Message ID must not be blank");
        }
    }
}
