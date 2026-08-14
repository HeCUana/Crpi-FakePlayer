package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.itemuse.ItemUseSession;
import net.minecraft.util.Hand;

/**
 * Holds an item use for a duration, then releases it through the vanilla
 * stop-using path (bow draw, eating, shield block). Stateful.
 */
public final class UseReleaseAction extends Action {
    private final Hand hand;
    private final long durationTicks;
    private ItemUseSession session;

    public UseReleaseAction(FakePlayerHandle handle, long currentTick, Hand hand, long durationTicks) {
        super(ActionType.USE_RELEASE, handle, currentTick);
        this.hand = hand;
        this.durationTicks = durationTicks;
    }

    public Hand hand() {
        return this.hand;
    }

    public long durationTicks() {
        return this.durationTicks;
    }

    public ItemUseSession session() {
        return this.session;
    }

    public void session(ItemUseSession session) {
        this.session = session;
    }
}
