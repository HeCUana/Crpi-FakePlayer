package com.crpi.fakeplayer.action;

/**
 * Lifecycle state of an {@link Action}.
 */
public enum ActionState {
    CREATED,
    QUEUED,
    STARTED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
}
