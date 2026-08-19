package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.ActionState;
import com.crpi.fakeplayer.action.impl.DigAction;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.mining.MiningManager;
import com.crpi.fakeplayer.mining.MiningSession;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * DIG via the vanilla {@code processBlockBreakingAction} + the manager's
 * own per-tick progress. execute() starts the session and returns RETRY so
 * the scheduler ticks it; tick() polls the session state.
 */
public final class DigExecutor implements ActionExecutor<DigAction> {
    private static final double REACH_SQ = 6.0 * 6.0;

    @Override
    public ActionResult execute(DigAction action, FakePlayerHandle handle) {
        if (!CRPIFakePlayerSettings.fakePlayerMining) {
            return ActionResult.NO_PERMISSION;
        }
        BlockPos pos = action.pos();
        if (!handle.world().isChunkLoaded(pos)) {
            return ActionResult.INVALID_TARGET;
        }
        double dx = pos.getX() + 0.5 - handle.x();
        double dy = pos.getY() + 0.5 - handle.y() - 1.62;
        double dz = pos.getZ() + 0.5 - handle.z();
        if (dx * dx + dy * dy + dz * dz > REACH_SQ) {
            return ActionResult.OUT_OF_RANGE;
        }
        BlockState state = handle.world().getBlockState(pos);
        if (state.isAir()) {
            return ActionResult.INVALID_TARGET;
        }
        MiningSession session = MiningManager.begin(handle, pos, action.direction(), handle.world().getServer().getTicks());
        action.session(session);
        // Creative mode breaks instantly inside START; check right away.
        if (handle.world().getBlockState(pos).isAir()) {
            MiningManager.finish(handle.player().getUuid());
            return ActionResult.SUCCESS;
        }
        if (state.calcBlockBreakingDelta(handle.player(), handle.world(), pos) <= 0) {
            session.cancel();
            MiningManager.finish(handle.player().getUuid());
            return ActionResult.FAIL;
        }
        return ActionResult.RETRY;
    }

    @Override
    public void tick(DigAction action, FakePlayerHandle handle) {
        MiningSession session = action.session();
        if (session == null) {
            action.state(ActionState.FAILED);
            action.result(ActionResult.INVALID_STATE);
            return;
        }
        MiningSession.State state = session.tick(handle.world().getServer().getTicks());
        switch (state) {
            case SUCCESS -> {
                action.state(ActionState.SUCCESS);
                action.result(ActionResult.SUCCESS);
            }
            case FAILED, CANCELLED -> {
                action.state(ActionState.FAILED);
                action.result(ActionResult.FAIL);
            }
            default -> {
                // still RUNNING
            }
        }
        // Drop the finished session so it isn't retained per mined block.
        // (MiningManager.finish removes the map entry; no-op if already gone.)
        if (action.state().isTerminal()) {
            MiningManager.finish(handle.player().getUuid());
        }
    }

    @Override
    public void cancel(DigAction action, FakePlayerHandle handle) {
        if (action.session() != null) {
            action.session().cancel();
        }
        MiningManager.finish(handle.player().getUuid());
        action.state(ActionState.CANCELLED);
    }
}
