package kuchma.mike.bbdm.application;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

public record ScriptingAdventureStateEvent(
    String sessionId,
    long gold,
    int score,
    int lives,
    int level,
    int turn,
    int hpotCount,
    String task,
    String message,
    boolean complete,
    String error
) {
    public static Builder builder() {
        return new Builder();
    }

    // It's a common pattern in builders to have method names the same as field names.
    // So PMD is suppressed.
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static final class Builder {
        private @Nullable String sessionId;
        private long gold;
        private int score;
        private int lives;
        private int level;
        private int turn;
        private int hpotCount;
        private @Nullable String task;
        private @Nullable String message;
        private boolean complete;
        private @Nullable String error;

        private Builder() {
        }

        public Builder sessionId(final String value) {
            sessionId = value;
            return this;
        }

        public Builder gold(final long value) {
            gold = value;
            return this;
        }

        public Builder score(final int value) {
            score = value;
            return this;
        }

        public Builder lives(final int value) {
            lives = value;
            return this;
        }

        public Builder level(final int value) {
            level = value;
            return this;
        }

        public Builder turn(final int value) {
            turn = value;
            return this;
        }

        public Builder hpotCount(final int value) {
            hpotCount = value;
            return this;
        }

        public Builder task(final String value) {
            task = value;
            return this;
        }

        public Builder message(final String value) {
            message = value;
            return this;
        }

        public Builder complete(final boolean value) {
            complete = value;
            return this;
        }

        public Builder error(final String value) {
            error = value;
            return this;
        }

        public ScriptingAdventureStateEvent build() {
            return new ScriptingAdventureStateEvent(
                Objects.requireNonNull(sessionId, "sessionId is required"),
                gold,
                score,
                lives,
                level,
                turn,
                hpotCount,
                Objects.requireNonNull(task, "task is required"),
                Objects.requireNonNull(message, "message is required"),
                complete,
                Objects.requireNonNull(error, "error is required")
            );
        }
    }
}
