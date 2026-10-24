package kuchma.mike.bbdm.domain.model;

public record MessageDecision(boolean accepted) {
    private static final String ACCEPTED_ACTION = "true";
    private static final String REJECTED_ACTION = "false";

    public static MessageDecision fromAction(String action) {
        if (ACCEPTED_ACTION.equals(action)) {
            return new MessageDecision(true);
        }
        if (REJECTED_ACTION.equals(action)) {
            return new MessageDecision(false);
        }
        throw new IllegalArgumentException("Message action must be either 'true' or 'false'");
    }

    public String toUpstreamValue() {
        return accepted ? ACCEPTED_ACTION : REJECTED_ACTION;
    }
}
