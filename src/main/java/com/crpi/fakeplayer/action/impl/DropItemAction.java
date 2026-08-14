package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.util.Hand;

/**
 * Drops an item through the vanilla player drop path
 * ({@code ServerPlayerEntity.dropItem}).
 */
public final class DropItemAction extends Action {
    private final Hand hand;
    private final boolean entireStack;

    public DropItemAction(FakePlayerHandle handle, long currentTick, Hand hand, boolean entireStack) {
        super(ActionType.DROP_ITEM, handle, currentTick);
        this.hand = hand;
        this.entireStack = entireStack;
    }

    public Hand hand() {
        return this.hand;
    }

    public boolean entireStack() {
        return this.entireStack;
    }
}
