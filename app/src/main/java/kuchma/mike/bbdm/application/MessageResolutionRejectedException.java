package kuchma.mike.bbdm.application;

import java.io.Serial;

public class MessageResolutionRejectedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String messageId;

    public MessageResolutionRejectedException(final String messageId) {
        super("The game server rejected this message. It has been removed from the message board.");
        this.messageId = messageId;
    }

    public String messageId() {
        return messageId;
    }
}
