package com.crpi.fakeplayer.action;

import java.util.EnumMap;
import java.util.Map;

/**
 * Routes an action to the executor registered for its {@link ActionType}.
 */
public final class ActionDispatcher {
    private final Map<ActionType, ActionExecutor<?>> executors = new EnumMap<>(ActionType.class);

    public <T extends Action> void register(ActionType type, ActionExecutor<T> executor) {
        this.executors.put(type, executor);
    }

    @SuppressWarnings("unchecked")
    public <T extends Action> ActionExecutor<T> executorFor(ActionType type) {
        return (ActionExecutor<T>) this.executors.get(type);
    }

    public boolean hasExecutor(ActionType type) {
        return this.executors.containsKey(type);
    }
}
