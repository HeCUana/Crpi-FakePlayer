package com.crpi.fakeplayer.fakeplayer;

import com.crpi.fakeplayer.scheduler.ActionScheduler;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
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

    /**
     * Online sweep run once per server tick (memory-leak guard). Every handle
     * originates here (via {@link #acquire} from {@code FakePlayerAdapter}),
     * so {@code HANDLES} is a superset of every fake player this mod has ever
     * touched. Dropping a gone player's handle through the scheduler releases
     * all per-bot registries at once, letting the {@link ServerPlayerEntity}
     * be garbage-collected instead of being held alive by cached state.
     */
    public static void tick(MinecraftServer server, ActionScheduler scheduler) {
        for (UUID uuid : new ArrayList<>(HANDLES.keySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null || player.isRemoved()) {
                scheduler.releasePlayer(uuid);
            }
        }
    }
}
