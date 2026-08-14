package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.GuiClickAction;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.container.ContainerContext;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * GUI_CLICK via the vanilla {@code ScreenHandler.onSlotClick} path.
 * Validates: container open, handler usable, slot in range, and a sane
 * button for the action type before clicking.
 */
public final class GuiExecutor implements ActionExecutor<GuiClickAction> {
    @Override
    public ActionResult execute(GuiClickAction action, FakePlayerHandle handle) {
        if (!CRPIFakePlayerSettings.fakePlayerContainer) {
            return ActionResult.NO_PERMISSION;
        }
        ContainerContext context = ContainerContext.of(handle);
        if (context == null) {
            return ActionResult.INVALID_STATE;
        }
        ScreenHandler handler = context.handler();
        if (!context.isOpen(handle.player())) {
            return ActionResult.INVALID_STATE;
        }
        int slot = action.slot();
        if (!context.isValidSlot(slot)) {
            return ActionResult.INVALID_TARGET;
        }
        SlotActionType type = action.actionType();
        int button = action.button();
        if (!validButton(type, button)) {
            return ActionResult.INVALID_TARGET;
        }
        handler.onSlotClick(slot, button, type, handle.player());
        return ActionResult.SUCCESS;
    }

    /** Button semantics per vanilla SlotActionType. */
    private static boolean validButton(SlotActionType type, int button) {
        return switch (type) {
            case PICKUP, QUICK_MOVE, CLONE, THROW, PICKUP_ALL -> button == 0;
            case SWAP -> button >= 0 && button <= 8;
            case QUICK_CRAFT -> button >= 0 && button <= 5;
        };
    }
}
