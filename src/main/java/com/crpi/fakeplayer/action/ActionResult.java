package com.crpi.fakeplayer.action;

/**
 * Terminal result of an action execution attempt.
 */
public enum ActionResult {
    SUCCESS,
    PASS,
    FAIL,
    RETRY,
    SKIP,
    ABORT,
    INVALID_TARGET,
    OUT_OF_RANGE,
    NO_PERMISSION,
    INVALID_STATE,
    CONCURRENCY_LIMIT;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
