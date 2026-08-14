package com.crpi.fakeplayer.scheduler;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionDispatcher;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.ActionState;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.fakeplayer.FakePlayerRegistry;
import com.crpi.fakeplayer.action.ActionExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives every queued/running action from the server tick (Carpet
 * {@code onTick}). No extra threads are created: stateful actions are
 * advanced here, on the server thread, once per tick.
 */
public final class ActionScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("CRPI-FakePlayer");

    private final ActionDispatcher dispatcher;
    private final Map<UUID, ActionQueue> queues = new ConcurrentHashMap<>();
    private final List<Action> running = new ArrayList<>();

    public ActionScheduler(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ActionDispatcher dispatcher() {
        return this.dispatcher;
    }

    public ActionQueue queueOf(FakePlayerHandle handle) {
        return this.queues.computeIfAbsent(handle.player().getUuid(), id -> new ActionQueue(handle));
    }

    public void releasePlayer(UUID uuid) {
        this.queues.remove(uuid);
        FakePlayerRegistry.release(uuid);
    }

    public void tick(MinecraftServer server) {
        long tick = server.getTicks();
        // 1. start queued actions
        for (ActionQueue queue : this.queues.values()) {
            Action next = queue.pollReady(tick);
            if (next != null) {
                start(next);
            }
        }
        // 2. advance running (stateful) actions
        for (int i = this.running.size() - 1; i >= 0; i--) {
            Action action = this.running.get(i);
            ActionExecutor<Action> executor = this.dispatcher.executorFor(action.type());
            if (executor == null) {
                finish(action, ActionResult.INVALID_STATE);
                continue;
            }
            executor.tick(action, action.handle());
            if (action.state().isTerminal()) {
                this.running.remove(i);
            }
        }
    }

    /** Executes an action immediately (used by the command/API entry points). */
    public ActionResult runNow(Action action) {
        start(action);
        return action.result() != null ? action.result() : ActionResult.PASS;
    }

    private void start(Action action) {
        if (!action.handle().isOnline()) {
            finish(action, ActionResult.INVALID_TARGET);
            return;
        }
        ActionExecutor<Action> executor = this.dispatcher.executorFor(action.type());
        if (executor == null) {
            finish(action, ActionResult.INVALID_STATE);
            return;
        }
        action.state(ActionState.STARTED);
        ActionResult result = executor.execute(action, action.handle());
        if (result == ActionResult.RETRY) {
            action.state(ActionState.RUNNING);
            this.running.add(action);
            return;
        }
        finish(action, result);
    }

    private void finish(Action action, ActionResult result) {
        action.result(result);
        action.state(result.isSuccess() ? ActionState.SUCCESS : ActionState.FAILED);
        if (CRPIFakePlayerSettings.fakePlayerDebug) {
            LOGGER.info("player={} action={} state={} result={}",
                action.handle().name(), action.type(), action.state(), result);
        }
    }
}
