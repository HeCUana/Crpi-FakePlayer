package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.AttackAction;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.entity.Entity;

/**
 * ATTACK via the vanilla {@code ServerPlayerEntity.attack} path.
 * Validates target liveness, same world and a 4-block reach before attacking.
 */
public final class AttackExecutor implements ActionExecutor<AttackAction> {
    private static final double REACH_SQ = 4.0 * 4.0;

    @Override
    public ActionResult execute(AttackAction action, FakePlayerHandle handle) {
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
        handle.player().attack(target);
        return ActionResult.SUCCESS;
    }
}
