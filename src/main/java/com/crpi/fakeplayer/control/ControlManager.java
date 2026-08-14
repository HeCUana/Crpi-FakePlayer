package com.crpi.fakeplayer.control;

import com.crpi.fakeplayer.fakeplayer.FakePlayerRegistry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Drives every {@link FakePlayerControl} task once per server tick.
 */
public final class ControlManager {
    private static final Map<UUID, FakePlayerControl> CONTROLS = new ConcurrentHashMap<>();

    private ControlManager() {
    }

    public static FakePlayerControl of(com.crpi.fakeplayer.fakeplayer.FakePlayerHandle handle) {
        return CONTROLS.computeIfAbsent(handle.player().getUuid(), id -> new FakePlayerControl(handle));
    }

    public static void tick(MinecraftServer server) {
        long tick = server.getTicks();
        for (Map.Entry<UUID, FakePlayerControl> entry : CONTROLS.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                entry.getValue().clearTask();
                CONTROLS.remove(entry.getKey());
                continue;
            }
            entry.getValue().tickTask(tick);
        }
    }

    public static void release(UUID uuid) {
        FakePlayerControl control = CONTROLS.remove(uuid);
        if (control != null) {
            control.clearTask();
        }
    }
}
