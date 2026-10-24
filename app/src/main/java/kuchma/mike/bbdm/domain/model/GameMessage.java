package kuchma.mike.bbdm.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.jspecify.annotations.Nullable;

public record GameMessage(
    MessageId adId,
    String message,
    int reward,
    int expiresIn,
    @Nullable String probability,
    @Nullable Integer encrypted
) {
    public GameMessage {
        if (encrypted == null) {
            encrypted = 0;
        }
        if (Integer.valueOf(1).equals(encrypted)) {
            adId = new MessageId(decode(adId.value()));
            message = decode(message);
            if (probability != null) {
                probability = decode(probability);
            }
        }
    }

    private static String decode(final String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
