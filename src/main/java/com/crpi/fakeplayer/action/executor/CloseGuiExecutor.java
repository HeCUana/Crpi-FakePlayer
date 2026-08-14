package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.CloseGuiAction;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;

/**
 * CLOSE_GUI via the vanilla {@code closeHandledScreen()} path, so
 * {@code ScreenHandler.onClosed} runs normally. No-op when the player has
 * no container open (only the default inventory screen).
 */
public final class CloseGuiExecutor implements ActionExecutor<CloseGuiAction> {
    @Override
    public ActionResult execute(CloseGuiAction action, FakePlayerHandle handle) {
        ScreenHandler handler = handle.currentScreenHandler();
        if (handler == null || handler instanceof PlayerScreenHandler) {
            return ActionResult.INVALID_STATE;
        }
        handle.player().closeHandledScreen();
        return ActionResult.SUCCESS;
    }
}
