package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.UseAction;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

/**
 * USE via the vanilla {@code interactBlock} path with a fully parameterised
 * {@link BlockHitResult}. Covers placement, containers, doors, buttons,
 * levers and any mod block using the standard interaction flow.
 */
public final class UseExecutor implements ActionExecutor<UseAction> {
    private static final double REACH_SQ = 6.0 * 6.0;

    @Override
    public ActionResult execute(UseAction action, FakePlayerHandle handle) {
        if (!CRPIFakePlayerSettings.fakePlayerInteraction) {
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
        BlockHitResult hit = new BlockHitResult(pos.toCenterPos(), action.direction(), pos, false);
        net.minecraft.util.ActionResult vanilla = handle.player().interactionManager.interactBlock(
            handle.player(),
            handle.world(),
            handle.player().getMainHandStack(),
            Hand.MAIN_HAND,
            hit
        );
        return vanilla == net.minecraft.util.ActionResult.FAIL ? ActionResult.FAIL : ActionResult.SUCCESS;
    }
}
