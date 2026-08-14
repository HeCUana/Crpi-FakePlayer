package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;

/**
 * Closes the fake player's open container through the vanilla
 * {@code closeHandledScreen()} path so {@code ScreenHandler.onClosed}
 * runs normally.
 */
public final class CloseGuiAction extends Action {
    public CloseGuiAction(FakePlayerHandle handle, long currentTick) {
        super(ActionType.CLOSE_GUI, handle, currentTick);
    }
}
