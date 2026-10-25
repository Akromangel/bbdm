package kuchma.mike.bbdm.application;

import java.io.Serial;

public class GameOverException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public GameOverException(final Throwable cause) {
        super("Game Over", cause);
    }
}
