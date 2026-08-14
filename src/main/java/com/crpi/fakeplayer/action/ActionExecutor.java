package com.crpi.fakeplayer.action;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;

/**
 * Executes actions of type {@code T}. Instant actions only need
 * {@link #execute}; stateful actions additionally receive {@link #tick}
 * every server tick until they finish, and {@link #cancel} on abort.
 */
public interface ActionExecutor<T extends Action> {
    ActionResult execute(T action, FakePlayerHandle handle);

    default void tick(T action, FakePlayerHandle handle) {
    }

    default void cancel(T action, FakePlayerHandle handle) {
    }
}
