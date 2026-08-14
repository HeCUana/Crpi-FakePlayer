package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;

/**
 * Interacts with an entity (villager trade, riding, feeding, ...) through
 * the vanilla {@code PlayerEntity.interact} path.
 */
public final class InteractEntityAction extends Action {
    private final Entity target;
    private final Hand hand;

    public InteractEntityAction(FakePlayerHandle handle, long currentTick, Entity target, Hand hand) {
        super(ActionType.INTERACT_ENTITY, handle, currentTick);
        this.target = target;
        this.hand = hand;
    }

    public Entity target() {
        return this.target;
    }

    public Hand hand() {
        return this.hand;
    }
}
