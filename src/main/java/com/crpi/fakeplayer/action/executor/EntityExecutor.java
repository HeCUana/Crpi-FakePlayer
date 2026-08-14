package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.InteractEntityAction;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.entity.Entity;

/**
 * INTERACT_ENTITY via the vanilla {@code PlayerEntity.interact} path
 * (villager trades, riding, feeding, ...). Validates liveness, world and
 * a 4-block reach before interacting.
 */
public final class EntityExecutor implements ActionExecutor<InteractEntityAction> {
    private static final double REACH_SQ = 4.0 * 4.0;

    @Override
    public ActionResult execute(InteractEntityAction action, FakePlayerHandle handle) {
        if (!CRPIFakePlayerSettings.fakePlayerInteraction) {
            return ActionResult.NO_PERMISSION;
        }
        Entity target = action.target();
        if (target == null || !target.isAlive()) {
            return ActionResult.INVALID_TARGET;
        }
        if (target.getEntityWorld() != handle.world()) {
            return ActionResult.INVALID_TARGET;
        }
        double dx = target.getX() - handle.x();
        double dy = target.getY() - handle.y();
        double dz = target.getZ() - handle.z();
        if (dx * dx + dy * dy + dz * dz > REACH_SQ) {
            return ActionResult.OUT_OF_RANGE;
        }
        net.minecraft.util.ActionResult vanilla = handle.player().interact(target, action.hand());
        return vanilla == net.minecraft.util.ActionResult.FAIL ? ActionResult.FAIL : ActionResult.SUCCESS;
    }
}
