package com.crpi.fakeplayer.action;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base class for every action. Concrete actions (see {@code action.impl})
 * carry their own parameters and are executed by the matching
 * {@link ActionExecutor} registered in the {@link ActionDispatcher}.
 */
public abstract class Action {
    private static final AtomicLong IDS = new AtomicLong(0);

    private final long id = IDS.incrementAndGet();
    private final ActionType type;
    private final FakePlayerHandle handle;
    private final long createdTick;
    private long scheduledTick;
    private ActionState state = ActionState.CREATED;
    private ActionResult result;

    protected Action(ActionType type, FakePlayerHandle handle, long currentTick) {
        this.type = type;
        this.handle = handle;
        this.createdTick = currentTick;
        this.scheduledTick = currentTick;
    }

    public long id() {
        return this.id;
    }

    public ActionType type() {
        return this.type;
    }

    public FakePlayerHandle handle() {
        return this.handle;
    }

    public long createdTick() {
        return this.createdTick;
    }

    public long scheduledTick() {
        return this.scheduledTick;
    }

    public void scheduledTick(long tick) {
        this.scheduledTick = tick;
    }

    public ActionState state() {
        return this.state;
    }

    public void state(ActionState state) {
        this.state = state;
    }

    public ActionResult result() {
        return this.result;
    }

    public void result(ActionResult result) {
        this.result = result;
    }
}
