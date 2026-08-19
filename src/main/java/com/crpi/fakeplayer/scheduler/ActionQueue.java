package com.crpi.fakeplayer.scheduler;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-fake-player action queue. Supports immediate execution (default),
 * delayed execution via {@link #schedule(Action, long)} and strict
 * sequential mode where a queued action waits for the previous one to finish.
 *
 * <p>All state is mutated only from the server thread via the scheduler tick.
 */
public final class ActionQueue {
    private final FakePlayerHandle handle;
    private final Deque<Action> queue = new ArrayDeque<>();
    private int runningCount;

    public ActionQueue(FakePlayerHandle handle) {
        this.handle = handle;
    }

    public FakePlayerHandle handle() {
        return this.handle;
    }

    public int size() {
        return this.queue.size();
    }

    /** Number of this bot's stateful actions currently being driven by the scheduler. */
    public int runningCount() {
        return this.runningCount;
    }

    /** Marks one of this bot's stateful actions as running. */
    void beginRunning() {
        this.runningCount++;
    }

    /** Marks one of this bot's stateful actions as finished. */
    void endRunning() {
        if (this.runningCount > 0) {
            this.runningCount--;
        }
    }

    /** Returns the action that should run now, or {@code null}. */
    public Action pollReady(long currentTick) {
        if (this.queue.isEmpty()) {
            return null;
        }
        Action head = this.queue.peek();
        if (head == null || head.scheduledTick() > currentTick) {
            return null;
        }
        this.queue.poll();
        return head;
    }

    public boolean offer(Action action) {
        if (this.size() >= CRPIFakePlayerSettings.maxQueueLength) {
            return false;
        }
        this.queue.offer(action);
        return true;
    }

    public void schedule(Action action, long delayTicks) {
        action.scheduledTick(action.createdTick() + delayTicks);
        this.offer(action);
    }

    public void clear() {
        this.queue.clear();
    }

    public boolean isFull() {
        return this.size() >= CRPIFakePlayerSettings.maxQueueLength;
    }
}
