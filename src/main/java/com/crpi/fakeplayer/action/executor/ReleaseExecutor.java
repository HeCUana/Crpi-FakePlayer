package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.ActionState;
import com.crpi.fakeplayer.action.impl.UseReleaseAction;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.itemuse.ItemUseSession;
import net.minecraft.item.ItemStack;

/**
 * USE_RELEASE: starts a vanilla item use ({@code setCurrentHand}), waits the
 * requested duration while the fake player's own tick advances the use, then
 * releases ({@code stopUsingItem}) so the item's vanilla
 * {@code onStoppedUsing} fires (bow shoots, food completes, shield drops).
 */
public final class ReleaseExecutor implements ActionExecutor<UseReleaseAction> {
    @Override
    public ActionResult execute(UseReleaseAction action, FakePlayerHandle handle) {
        if (!CRPIFakePlayerSettings.fakePlayerItemUse) {
            return ActionResult.NO_PERMISSION;
        }
        ItemStack stack = handle.player().getStackInHand(action.hand());
        if (stack.isEmpty()) {
            return ActionResult.INVALID_STATE;
        }
        ItemUseSession session = new ItemUseSession(handle, action.hand(), action.durationTicks());
        action.session(session);
        session.start(handle.world().getServer().getTicks());
        if (session.state() == ItemUseSession.State.FINISHED) {
            // item cannot be "held" (no use duration), e.g. snowball: fall back
            // to a plain interactItem so throwing still works
            net.minecraft.util.ActionResult vanilla = handle.player().interactionManager
                .interactItem(handle.player(), handle.world(), stack, action.hand());
            return vanilla == net.minecraft.util.ActionResult.FAIL ? ActionResult.FAIL : ActionResult.SUCCESS;
        }
        return ActionResult.RETRY;
    }

    @Override
    public void tick(UseReleaseAction action, FakePlayerHandle handle) {
        ItemUseSession session = action.session();
        if (session == null) {
            action.state(ActionState.FAILED);
            action.result(ActionResult.INVALID_STATE);
            return;
        }
        ItemUseSession.State state = session.tick(handle.world().getServer().getTicks());
        if (state == ItemUseSession.State.RELEASED || state == ItemUseSession.State.FINISHED) {
            action.state(ActionState.SUCCESS);
            action.result(ActionResult.SUCCESS);
        } else if (state == ItemUseSession.State.CANCELLED) {
            action.state(ActionState.FAILED);
            action.result(ActionResult.ABORT);
        }
    }

    @Override
    public void cancel(UseReleaseAction action, FakePlayerHandle handle) {
        if (action.session() != null) {
            action.session().cancel();
        }
        action.state(ActionState.CANCELLED);
    }
}
