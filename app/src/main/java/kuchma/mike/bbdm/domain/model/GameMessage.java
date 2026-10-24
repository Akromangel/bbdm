package kuchma.mike.bbdm.domain.model;

import org.jspecify.annotations.Nullable;

public record GameMessage(
    MessageId adId,
    String message,
    int reward,
    int expiresIn,
    @Nullable String probability
) {
}
