package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.util.Hand;

/**
 * Uses the item in the given hand through the vanilla
 * {@code ServerPlayerInteractionManager.interactItem} path.
 */
public final class UseItemAction extends Action {
    private final Hand hand;

    public UseItemAction(FakePlayerHandle handle, long currentTick, Hand hand) {
        super(ActionType.USE_ITEM, handle, currentTick);
        this.hand = hand;
    }

    public Hand hand() {
        return this.hand;
    }
}
