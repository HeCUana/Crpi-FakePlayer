package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.UseItemAction;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;

/**
 * USE_ITEM via the vanilla
 * {@code ServerPlayerInteractionManager.interactItem} path. The item itself
 * decides its behaviour (food, potion, snowball, ...).
 */
public final class UseItemExecutor implements ActionExecutor<UseItemAction> {
    @Override
    public ActionResult execute(UseItemAction action, FakePlayerHandle handle) {
        var player = handle.player();
        var world = handle.world();
        var stack = player.getStackInHand(action.hand());
        if (stack.isEmpty()) {
            return ActionResult.INVALID_STATE;
        }
        net.minecraft.util.ActionResult result = player.interactionManager.interactItem(player, world, stack, action.hand());
        return result == net.minecraft.util.ActionResult.FAIL ? ActionResult.FAIL : ActionResult.SUCCESS;
    }
}
