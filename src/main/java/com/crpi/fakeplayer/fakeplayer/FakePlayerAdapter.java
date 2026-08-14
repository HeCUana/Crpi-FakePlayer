package com.crpi.fakeplayer.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Adapts Carpet fake players to {@link FakePlayerHandle}.
 * This is the ONLY class in the mod that references Carpet internals
 * directly (the {@code EntityPlayerMPFake} class check).
 */
public final class FakePlayerAdapter {
    private FakePlayerAdapter() {
    }

    public static boolean isFakePlayer(ServerPlayerEntity player) {
        return player instanceof EntityPlayerMPFake;
    }

    /**
     * Resolves a fake player by name. Returns {@code null} when the player is
     * offline or is a real player.
     */
    public static FakePlayerHandle resolve(MinecraftServer server, String name) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
        if (player == null || !isFakePlayer(player)) {
            return null;
        }
        return FakePlayerRegistry.acquire(player);
    }
}
