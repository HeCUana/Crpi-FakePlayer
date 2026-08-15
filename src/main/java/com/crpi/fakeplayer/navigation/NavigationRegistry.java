package com.crpi.fakeplayer.navigation;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Per-fake-player NavigationManager registry, ticked every server tick.
 */
public final class NavigationRegistry {
    private static final Map<UUID, NavigationManager> MANAGERS = new ConcurrentHashMap<>();

    private NavigationRegistry() {
    }

    public static NavigationManager of(FakePlayerHandle handle) {
        return MANAGERS.computeIfAbsent(handle.player().getUuid(), id -> new NavigationManager(handle));
    }

    public static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, NavigationManager> entry : MANAGERS.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                entry.getValue().stop();
                MANAGERS.remove(entry.getKey());
                continue;
            }
            entry.getValue().tick();
        }
    }
}
