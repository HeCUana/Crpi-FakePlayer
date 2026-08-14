package com.crpi.fakeplayer.fakeplayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Caches one {@link FakePlayerHandle} per live fake player entity.
 */
public final class FakePlayerRegistry {
    private static final Map<UUID, FakePlayerHandle> HANDLES = new ConcurrentHashMap<>();

    private FakePlayerRegistry() {
    }

    public static FakePlayerHandle acquire(ServerPlayerEntity player) {
        return HANDLES.computeIfAbsent(player.getUuid(), id -> new FakePlayerHandle(player));
    }

    /** Called when a player disconnects; drop the cached handle. */
    public static void release(UUID uuid) {
        HANDLES.remove(uuid);
    }
}
