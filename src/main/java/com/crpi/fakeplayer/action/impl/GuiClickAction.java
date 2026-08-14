package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Clicks a slot in the fake player's currently open container through the
 * vanilla {@code ScreenHandler.onSlotClick} path.
 */
public final class GuiClickAction extends Action {
    private final int slot;
    private final int button;
    private final SlotActionType actionType;

    public GuiClickAction(FakePlayerHandle handle, long currentTick, int slot, int button, SlotActionType actionType) {
        super(ActionType.GUI_CLICK, handle, currentTick);
        this.slot = slot;
        this.button = button;
        this.actionType = actionType;
    }

    public int slot() {
        return this.slot;
    }

    public int button() {
        return this.button;
    }

    public SlotActionType actionType() {
        return this.actionType;
    }
}
