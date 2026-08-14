package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.entity.Entity;

/**
 * Attacks an entity using the vanilla player attack path
 * ({@code ServerPlayerEntity.attack}), which covers damage, knockback,
 * enchantments and attack cooldown.
 */
public final class AttackAction extends Action {
    private final Entity target;

    public AttackAction(FakePlayerHandle handle, long currentTick, Entity target) {
        super(ActionType.ATTACK, handle, currentTick);
        this.target = target;
    }

    public Entity target() {
        return this.target;
    }
}
