package kuchma.mike.bbdm.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MessageDecision(@JsonIgnore boolean accepted) {
    private static final String ACCEPTED_ACTION = "true";
    private static final String REJECTED_ACTION = "false";

    @JsonCreator
    public static MessageDecision fromAction(@JsonProperty("action") String action) {
        if (ACCEPTED_ACTION.equals(action)) {
            return new MessageDecision(true);
        }
        if (REJECTED_ACTION.equals(action)) {
            return new MessageDecision(false);
        }
        throw new IllegalArgumentException("Message action must be either 'true' or 'false'");
    }

    @JsonProperty("action")
    public String toUpstreamValue() {
        return accepted ? ACCEPTED_ACTION : REJECTED_ACTION;
    }
}
