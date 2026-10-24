package kuchma.mike.bbdm.domain.model;

import org.jspecify.annotations.Nullable;

public record GameMessage(
    MessageId adId,
    String message,
    String reward,
    int expiresIn,
    @Nullable String probability
) {
}
