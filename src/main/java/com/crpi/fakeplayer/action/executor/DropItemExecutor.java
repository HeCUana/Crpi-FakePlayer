package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionExecutor;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.DropItemAction;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * DROP_ITEM via the vanilla {@code ServerPlayerEntity.dropItem} path.
 * Supports main/off hand and single/entire stack.
 */
public final class DropItemExecutor implements ActionExecutor<DropItemAction> {
    @Override
    public ActionResult execute(DropItemAction action, FakePlayerHandle handle) {
        PlayerInventory inventory = handle.inventory();
        int slot = action.hand() == net.minecraft.util.Hand.OFF_HAND
            ? PlayerInventory.OFF_HAND_SLOT
            : inventory.getSelectedSlot();
        ItemStack stack = inventory.getStack(slot);
        if (stack.isEmpty()) {
            return ActionResult.INVALID_STATE;
        }
        ItemStack dropped = action.entireStack()
            ? inventory.removeStack(slot)
            : inventory.removeStack(slot, 1);
        if (dropped.isEmpty()) {
            return ActionResult.FAIL;
        }
        handle.player().dropItem(dropped, false, true);
        return ActionResult.SUCCESS;
    }
}
