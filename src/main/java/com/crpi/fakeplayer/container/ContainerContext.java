package com.crpi.fakeplayer.container;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A snapshot of the container a fake player currently has open, resolved
 * on demand from the player's live {@code currentScreenHandler}. No extra
 * state is kept: the ScreenHandler itself is the source of truth, so mod
 * containers work as long as they use the standard handler flow.
 *
 * @param syncId    handler sync id
 * @param handler   the live handler (never {@code null})
 * @param slotCount number of slots
 */
public record ContainerContext(int syncId, ScreenHandler handler, int slotCount) {

    /**
     * Resolves the current container of a fake player.
     *
     * @return the context, or {@code null} when no container is open
     *         (default inventory screen does not count)
     */
    public static ContainerContext of(FakePlayerHandle handle) {
        ScreenHandler handler = handle.currentScreenHandler();
        if (handler == null || handler instanceof PlayerScreenHandler) {
            return null;
        }
        return new ContainerContext(handler.syncId, handler, handler.slots.size());
    }

    public boolean isOpen(ServerPlayerEntity player) {
        return player.currentScreenHandler == this.handler && this.handler.canUse(player);
    }

    /** True when the given slot index is within this container. */
    public boolean isValidSlot(int slot) {
        return slot >= 0 && slot < this.slotCount;
    }
}
